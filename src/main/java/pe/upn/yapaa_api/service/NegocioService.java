package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.negocio.NegocioRequest;
import pe.upn.yapaa_api.dto.negocio.NegocioResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.repository.CuentaRepository;
import pe.upn.yapaa_api.repository.NegocioRepository;

import java.util.List;
import pe.upn.yapaa_api.security.UsuarioActual;
import pe.upn.yapaa_api.repository.RolRepository;
import pe.upn.yapaa_api.dto.negocio.AfiliacionRequest;

@Service
public class NegocioService {

    private static final List<String> ESTADOS_VALIDOS =
            List.of("PENDIENTE", "APROBADO", "RECHAZADO", "SUSPENDIDO");

    private final NegocioRepository negocioRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioActual usuarioActual;
    private final ConsultaRucService consultaRuc;
    private final PlanService planes;


    public NegocioService(NegocioRepository negocioRepository, CuentaRepository cuentaRepository, UsuarioActual usuarioActual, ConsultaRucService consultaRuc, PlanService planes) {
        this.negocioRepository = negocioRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioActual = usuarioActual;
        this.consultaRuc=consultaRuc;
        this.planes=planes;

    }

    @Transactional(readOnly = true)
    public Result<Page<NegocioResponse>> listar(String estadoAprobacion, Pageable pageable) {
        if (!usuarioActual.esAdministrador()) {
            return Result.exitoso(negocioRepository.findByEstadoAprobacionAndActivoTrue("APROBADO", pageable).map(this::aRespuesta));
        }
        Page<Negocio> pagina = (estadoAprobacion == null || estadoAprobacion.isBlank())
                ? negocioRepository.findAll(pageable)
                : negocioRepository.findByEstadoAprobacion(estadoAprobacion.toUpperCase(), pageable);
        return Result.exitoso(pagina.map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public Result<Page<NegocioResponse>> listarPorCuenta(Integer idCuenta, Pageable pageable) {
        usuarioActual.verificarCuenta(idCuenta);
        Page<NegocioResponse> pagina = negocioRepository.findByCuentaIdCuenta(idCuenta, pageable)
                .map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<NegocioResponse> obtenerPorId(Integer id) {
        Negocio negocio = buscarPorId(id);
        if (!Boolean.TRUE.equals(negocio.getActivo()) || !"APROBADO".equals(negocio.getEstadoAprobacion())) {
            usuarioActual.verificarNegocio(negocio);
        }
        return Result.exitoso(aRespuesta(negocio));
    }

    @Transactional
    public Result<NegocioResponse> crear(NegocioRequest request) {
        usuarioActual.verificarCuenta(request.getIdCuenta());
        Cuenta cuenta = cuentaRepository.findById(request.getIdCuenta())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada."));

        if (negocioRepository.existsByRuc(request.getRuc())) {
            throw new ReglaNegocioException("Ya existe un negocio registrado con ese RUC.");
        }
        validarRucConTipoContribuyente(request.getRuc(), request.getTipoContribuyente());

        Negocio negocio = new Negocio();
        negocio.setCuenta(cuenta);
        aplicarDatos(negocio, request);
        negocio.setEstadoAprobacion("PENDIENTE");
        negocio.setActivo(Boolean.TRUE);

        Negocio guardado = negocioRepository.saveAndFlush(negocio);
        planes.vigente(guardado.getIdNegocio());
        return Result.exitoso("Negocio registrado. Queda pendiente de aprobacion.", aRespuesta(guardado));
    }

    @Transactional
    public Result<NegocioResponse> actualizar(Integer id, NegocioRequest request) {
        Negocio negocio = buscarPorId(id);
        usuarioActual.verificarNegocio(negocio);
        if (!negocio.getRuc().equals(request.getRuc()) && negocioRepository.existsByRuc(request.getRuc())) {
            throw new ReglaNegocioException("Ya existe otro negocio registrado con ese RUC.");
        }
        validarRucConTipoContribuyente(request.getRuc(), request.getTipoContribuyente());
        if(!negocio.getRuc().equals(request.getRuc())) {
            var datos=consultaRuc.consultar(request.getRuc());
            if(!"ACTIVO".equalsIgnoreCase(datos.estado()) || !"HABIDO".equalsIgnoreCase(datos.condicion())) throw new ReglaNegocioException("El RUC debe estar ACTIVO y HABIDO.");
            request.setRazonSocial(datos.razonSocial());
        }else request.setRazonSocial(negocio.getRazonSocial());
        aplicarDatos(negocio, request);
        Negocio actualizado = negocioRepository.save(negocio);
        return Result.exitoso("Negocio actualizado correctamente.", aRespuesta(actualizado));
    }

    @Transactional
    public Result<NegocioResponse> cambiarEstadoAprobacion(Integer id, String estadoAprobacion) {
        String estado = estadoAprobacion == null ? "" : estadoAprobacion.toUpperCase();
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new ReglaNegocioException("Estado de aprobacion invalido: " + estadoAprobacion);
        }
        Negocio negocio = buscarPorId(id);
        negocio.setEstadoAprobacion(estado);
        Negocio actualizado = negocioRepository.save(negocio);
        return Result.exitoso("Estado de aprobacion actualizado.", aRespuesta(actualizado));
    }

    @Transactional
    public Result<String> darDeBaja(Integer id) {
        Negocio negocio = buscarPorId(id);
        usuarioActual.verificarNegocio(negocio);
        negocio.setActivo(Boolean.FALSE);
        negocioRepository.save(negocio);
        return Result.exitoso("Negocio dado de baja.", "Negocio " + id + " desactivado.");
    }

    @Transactional
    public Result<NegocioResponse> registrarNuevaCuenta(Cuenta cuenta, AfiliacionRequest request, String razonSocial) {
        if (negocioRepository.existsByRuc(request.getRuc())) throw new ReglaNegocioException("Ya existe un negocio con ese RUC.");
        validarRucConTipoContribuyente(request.getRuc(), request.getTipoContribuyente());
        Negocio negocio = new Negocio();
        negocio.setCuenta(cuenta); negocio.setNombreComercial(request.getNombreComercial());
        negocio.setRazonSocial(razonSocial); negocio.setRuc(request.getRuc());
        negocio.setTipoContribuyente(request.getTipoContribuyente());
        negocio.setDireccion(request.getDireccion()); negocio.setDistrito(request.getDistrito());
        negocio.setCiudad(request.getCiudad()); negocio.setTelefonoContacto(request.getTelefonoContacto());
        negocio.setEstadoAprobacion("PENDIENTE"); negocio.setActivo(true);
        Negocio guardado=negocioRepository.saveAndFlush(negocio);
        planes.vigente(guardado.getIdNegocio());
        return Result.exitoso(aRespuesta(guardado));
    }

    @Transactional(readOnly = true)
    public Result<Page<NegocioResponse>> misNegocios(Pageable pageable) {
        return listarPorCuenta(usuarioActual.obtenerCuenta().getIdCuenta(), pageable);
    }

    private void aplicarDatos(Negocio negocio, NegocioRequest request) {
        negocio.setNombreComercial(request.getNombreComercial());
        negocio.setRazonSocial(request.getRazonSocial());
        negocio.setTipoContribuyente(request.getTipoContribuyente());
        negocio.setRuc(request.getRuc());
        negocio.setDireccion(request.getDireccion());
        negocio.setDistrito(request.getDistrito());
        negocio.setCiudad(request.getCiudad());
        negocio.setTelefonoContacto(request.getTelefonoContacto());
    }

    private void validarRucConTipoContribuyente(String ruc, String tipoContribuyente) {
        if ("PERSONA_NATURAL".equals(tipoContribuyente) && !ruc.startsWith("10")) {
            throw new ReglaNegocioException("El RUC de una persona natural debe iniciar con 10.");
        }
        if ("PERSONA_JURIDICA".equals(tipoContribuyente) && !ruc.startsWith("20")) {
            throw new ReglaNegocioException("El RUC de una persona juridica debe iniciar con 20.");
        }
    }

    private Negocio buscarPorId(Integer id) {
        return negocioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado: " + id));
    }

    private NegocioResponse aRespuesta(Negocio negocio) {
        NegocioResponse respuesta = new NegocioResponse();
        respuesta.setIdNegocio(negocio.getIdNegocio());
        if (negocio.getCuenta() != null && (usuarioActual.esAdministrador() || usuarioActual.esPropietario(negocio.getCuenta().getIdCuenta()))) {
            respuesta.setIdCuenta(negocio.getCuenta().getIdCuenta());
            respuesta.setCorreoPropietario(negocio.getCuenta().getCorreo());
        }
        respuesta.setNombreComercial(negocio.getNombreComercial());
        respuesta.setRazonSocial(negocio.getRazonSocial());
        respuesta.setTipoContribuyente(negocio.getTipoContribuyente());
        respuesta.setRuc(negocio.getRuc());
        respuesta.setDireccion(negocio.getDireccion());
        respuesta.setDistrito(negocio.getDistrito());
        respuesta.setCiudad(negocio.getCiudad());
        respuesta.setTelefonoContacto(negocio.getTelefonoContacto());
        respuesta.setEstadoAprobacion(negocio.getEstadoAprobacion());
        respuesta.setActivo(negocio.getActivo());
        respuesta.setFechaRegistro(negocio.getFechaRegistro());
        return respuesta;
    }
}
