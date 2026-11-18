package pe.upn.yapaa_api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import pe.upn.yapaa_api.model.Cliente;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.repository.CuentaRepository;
import pe.upn.yapaa_api.repository.NegocioRepository;
import pe.upn.yapaa_api.repository.RolRepository;

@Configuration
@ConditionalOnProperty(name = "yapaa.demo.habilitado", havingValue = "true")
public class DatosIniciales {
    @Bean
    public CommandLineRunner crearUsuariosDePrueba(CuentaRepository cuentas, RolRepository roles,
            NegocioRepository negocios, PasswordEncoder encoder, PlatformTransactionManager gestor) {
        return args -> new TransactionTemplate(gestor).executeWithoutResult(transaccion -> {
            crearCuenta(cuentas, roles, encoder, "cliente@yapaa.test", "123", "Cliente", "CLIENTE");
            Cuenta propietario = crearCuenta(cuentas, roles, encoder, "negocio@yapaa.test", "1234", "Negocio", "NEGOCIO");
            crearCuenta(cuentas, roles, encoder, "admin@yapaa.test", "admin123", "Administrador", "ADMIN");
            // Preserva la cuenta de prueba anterior y sus datos.
            crearCuenta(cuentas, roles, encoder, "prueba@gmail.com", "prueba123", "Usuario", "CLIENTE");
            if (negocios.findByRuc("20000000001").isEmpty()) {
                Negocio negocio = new Negocio();
                negocio.setCuenta(propietario);
                negocio.setNombreComercial("Negocio de prueba YAPAA");
                negocio.setRazonSocial("Datos ficticios para demostracion");
                negocio.setTipoContribuyente("PERSONA_JURIDICA");
                negocio.setRuc("20000000001");
                negocio.setDireccion("Direccion de demostracion");
                negocio.setDistrito("Trujillo");
                negocio.setCiudad("Trujillo");
                negocio.setTelefonoContacto("900000002");
                negocio.setEstadoAprobacion("APROBADO");
                negocio.setActivo(true);
                negocios.save(negocio);
            }
        });
    }

    private Cuenta crearCuenta(CuentaRepository cuentas, RolRepository roles, PasswordEncoder encoder,
                               String correo, String clave, String nombre, String... nombresRoles) {
        Cuenta cuenta = cuentas.findByCorreo(correo).orElse(null);
        if (cuenta != null && correo.equals("prueba@gmail.com")) { return cuenta; }
        if (cuenta == null) { cuenta = new Cuenta(); cuenta.setCorreo(correo); }
        cuenta.setContrasenaHash(encoder.encode(clave));
        cuenta.setEstado("ACTIVO");
        cuenta.getRoles().clear();
        for (String rol : nombresRoles) {
            cuenta.getRoles().add(roles.findByNombre(rol)
                    .orElseThrow(() -> new IllegalStateException("Falta configurar el rol " + rol)));
        }
        if (cuenta.getCliente() == null) {
            Cliente cliente = new Cliente();
            cliente.setCuenta(cuenta); cliente.setNombres(nombre);
            cliente.setApellidos("Demostracion"); cliente.setTelefono("900000001");
            cuenta.setCliente(cliente);
        }
        return cuentas.save(cuenta);
    }
}