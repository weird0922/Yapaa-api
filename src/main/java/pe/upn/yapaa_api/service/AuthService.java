package pe.upn.yapaa_api.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.auth.LoginRequest;
import pe.upn.yapaa_api.dto.auth.LoginResponse;
import pe.upn.yapaa_api.dto.auth.RegistroRequest;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Cliente;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.Rol;
import pe.upn.yapaa_api.repository.CuentaRepository;
import pe.upn.yapaa_api.repository.RolRepository;
import pe.upn.yapaa_api.security.JwtService;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import pe.upn.yapaa_api.security.UsuarioActual;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CuentaRepository cuentaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioActual usuarioActual;
    private final NegocioService negocioService;
    private final ConsultaRucService consultaRuc;
    private final VerificacionCorreoService verificacion;
    @Value("${yapaa.demo.habilitado:false}")
    private boolean demoHabilitado;
    @Value("${yapaa.admin.correo:admin@admin.com}") private String administrador;

    public AuthService(AuthenticationManager authenticationManager,
                       CuentaRepository cuentaRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService, UsuarioActual usuarioActual, NegocioService negocioService, ConsultaRucService consultaRuc, VerificacionCorreoService verificacion) {
        this.authenticationManager = authenticationManager;
        this.cuentaRepository = cuentaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioActual = usuarioActual;
        this.negocioService = negocioService;
        this.consultaRuc = consultaRuc;
        this.verificacion=verificacion;
    }

    public Result<LoginResponse> login(LoginRequest request) {
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizarUsuario(request.getCorreo()), request.getContrasena()));

        Cuenta cuenta = cuentaRepository.findByCorreo(autenticacion.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada."));

        return Result.exitoso("Inicio de sesion exitoso.", construirRespuesta(cuenta));
    }

    @Transactional
    public Result<LoginResponse> registrar(RegistroRequest request) {
        request.setCorreo(VerificacionCorreoService.normalizar(request.getCorreo()));
        if(request.getCorreo().strip().equalsIgnoreCase(administrador))throw new ReglaNegocioException("Este correo no esta disponible para registro.");
        ValidacionRegistro.clave(request.getContrasena(),request.getConfirmarContrasena());
        request.setNombres(ValidacionRegistro.nombre(request.getNombres()));request.setApellidos(ValidacionRegistro.nombre(request.getApellidos()));
        request.setTelefono(ValidacionRegistro.telefono(request.getTelefono()));
        if (cuentaRepository.existsByCorreo(request.getCorreo())) {
            throw new ReglaNegocioException("El correo ya se encuentra registrado.");
        }

        String nombreRol = request.getRol() == null ? "CLIENTE" : request.getRol().toUpperCase();
        if (!nombreRol.equals("CLIENTE")) {
            throw new ReglaNegocioException("Crea una cuenta de cliente y solicita tu afiliacion desde el perfil.");
        }

        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado: " + nombreRol));

        Cuenta cuenta = new Cuenta();
        cuenta.setCorreo(request.getCorreo());
        cuenta.setContrasenaHash(passwordEncoder.encode(request.getContrasena()));
        cuenta.setEstado("ACTIVO");
        cuenta.getRoles().add(rol);

        if (nombreRol.equals("CLIENTE")) {
            if (esBlanco(request.getNombres()) || esBlanco(request.getApellidos())
                    || esBlanco(request.getTelefono())) {
                throw new ReglaNegocioException(
                        "Los nombres, apellidos y telefono son obligatorios para registrar un cliente.");
            }
            Cliente cliente = new Cliente();
            cliente.setCuenta(cuenta);
            cliente.setNombres(request.getNombres());
            cliente.setApellidos(request.getApellidos());
            cliente.setTelefono(request.getTelefono());
            cuenta.setCliente(cliente);
        }

        Cuenta guardada = cuentaRepository.save(cuenta);
        verificacion.consumir(request.getCorreo(),request.getTokenVerificacion());

        return Result.exitoso("Registro exitoso.", construirRespuesta(guardada));
    }

    @Transactional
    public Result<LoginResponse> registrarNegocio(pe.upn.yapaa_api.dto.auth.RegistroNegocioRequest request) {
        String correo = VerificacionCorreoService.normalizar(request.getCorreo());
        if(request.getCorreo().strip().equalsIgnoreCase(administrador))throw new ReglaNegocioException("Este correo no esta disponible para registro.");
        ValidacionRegistro.clave(request.getContrasena(),request.getConfirmarContrasena());
        ValidacionRegistro.telefono(request.getTelefonoContacto());
        if (cuentaRepository.existsByCorreo(correo)) {
            throw new ReglaNegocioException("Usa un correo distinto al de tu cuenta de cliente. Las cuentas de cliente y negocio son independientes.");
        }
        var datos = consultaRuc.consultar(request.getRuc());
        if (!"ACTIVO".equalsIgnoreCase(datos.estado()) || !"HABIDO".equalsIgnoreCase(datos.condicion())) {
            throw new ReglaNegocioException("El RUC debe estar ACTIVO y HABIDO para solicitar la afiliacion.");
        }
        Cuenta cuenta = new Cuenta();
        cuenta.setCorreo(correo); cuenta.setContrasenaHash(passwordEncoder.encode(request.getContrasena()));
        cuenta.setEstado("ACTIVO");
        cuenta.getRoles().add(rolRepository.findByNombre("NEGOCIO")
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol NEGOCIO no encontrado.")));
        Cuenta guardada = cuentaRepository.save(cuenta);
        negocioService.registrarNuevaCuenta(guardada, request, datos.razonSocial());
        verificacion.consumir(correo,request.getTokenVerificacion());
        return Result.exitoso("Cuenta de negocio creada. La solicitud queda pendiente de aprobacion.", construirRespuesta(guardada));
    }

    @Transactional(readOnly = true)
    public Result<LoginResponse> sesionActual() {
        return Result.exitoso(construirRespuesta(usuarioActual.obtenerCuenta()));
    }

    private String normalizarUsuario(String usuario) {
        String valor = usuario.trim().toLowerCase(java.util.Locale.ROOT);
        if (demoHabilitado && java.util.Set.of("cliente", "negocio", "admin").contains(valor)) {
            return valor + "@yapaa.test";
        }
        return valor;
    }

    private LoginResponse construirRespuesta(Cuenta cuenta) {
        List<String> roles = cuenta.getRoles().stream()
                .map(Rol::getNombre)
                .filter(rol->!rol.equals("ADMIN")||cuenta.getCorreo().equalsIgnoreCase(administrador))
                .collect(Collectors.toList());
        String token = jwtService.generarToken(cuenta.getCorreo(), roles);

        String nombres = cuenta.getCliente() != null ? cuenta.getCliente().getNombres() : roles.contains("NEGOCIO") ? "Negocio" : "Administrador";
        return new LoginResponse(token, cuenta.getIdCuenta(), cuenta.getCorreo(), nombres, roles);
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
