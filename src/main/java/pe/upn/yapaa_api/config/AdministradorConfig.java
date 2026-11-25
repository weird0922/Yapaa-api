package pe.upn.yapaa_api.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.repository.*;
@Configuration
public class AdministradorConfig {
 @Bean public CommandLineRunner configurarAdministrador(CuentaRepository cuentas,RolRepository roles,PasswordEncoder encoder,PlatformTransactionManager tx,@Value("${yapaa.admin.correo:admin@admin.com}") String correo,@Value("${yapaa.admin.clave:}") String clave){return args->new TransactionTemplate(tx).executeWithoutResult(t->{
  if(clave.isBlank())return;
  var cuenta=cuentas.findByCorreo(correo).orElseGet(Cuenta::new);
  if(cuenta.getIdCuenta()==null){cuenta.setCorreo(correo);cuenta.setContrasenaHash(encoder.encode(clave));cuenta.setEstado("ACTIVO");}
  cuenta.getRoles().clear();cuenta.getRoles().add(roles.findByNombre("ADMIN").orElseThrow());cuentas.save(cuenta);
 });}
}
