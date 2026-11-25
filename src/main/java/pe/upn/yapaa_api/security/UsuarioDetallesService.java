package pe.upn.yapaa_api.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.Rol;
import pe.upn.yapaa_api.repository.CuentaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioDetallesService implements UserDetailsService {

    private final CuentaRepository cuentaRepository;
    @org.springframework.beans.factory.annotation.Value("${yapaa.demo.habilitado:false}")
    private boolean demoHabilitado;
    @org.springframework.beans.factory.annotation.Value("${yapaa.admin.correo:admin@admin.com}") private String administrador;

    public UsuarioDetallesService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Cuenta cuenta = cuentaRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta no encontrada: " + correo));

        boolean esDemostracion = java.util.Set.of("cliente@yapaa.test", "negocio@yapaa.test", "admin@yapaa.test", "prueba@gmail.com")
                .contains(cuenta.getCorreo().toLowerCase(java.util.Locale.ROOT));
        List<SimpleGrantedAuthority> autoridades = cuenta.getRoles().stream()
                .map(Rol::getNombre)
                .filter(nombre -> !nombre.equals("ADMIN") || cuenta.getCorreo().equalsIgnoreCase(administrador))
                .map(nombre -> new SimpleGrantedAuthority("ROLE_" + nombre))
                .collect(Collectors.toList());

        return User.builder()
                .username(cuenta.getCorreo())
                .password(cuenta.getContrasenaHash())
                .authorities(autoridades)
                .disabled(!"ACTIVO".equals(cuenta.getEstado()) || (esDemostracion && !demoHabilitado) || (cuenta.getRoles().stream().anyMatch(r->r.getNombre().equals("ADMIN"))&&!cuenta.getCorreo().equalsIgnoreCase(administrador)))
                .build();
    }
}
