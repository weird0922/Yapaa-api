package pe.upn.yapaa_api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.model.Pedido;
import pe.upn.yapaa_api.repository.CuentaRepository;
import pe.upn.yapaa_api.model.Cuenta;

@Component
public class UsuarioActual {

    private final CuentaRepository cuentaRepository;

    public UsuarioActual(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    public String obtenerCorreo() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return null;
        }
        return autenticacion.getName();
    }

    public Cuenta obtenerCuenta() {
        Cuenta cuenta = cuentaRepository.findByCorreo(obtenerCorreo())
                .orElseThrow(() -> new AccessDeniedException("Inicia sesion para continuar."));
        return cuenta;
    }

    public boolean esAdministrador() { return tieneRol("ADMIN"); }

    public boolean esPropietario(Integer idCuenta) {
        if (obtenerCorreo() == null || "anonymousUser".equals(obtenerCorreo())) { return false; }
        return obtenerCuenta().getIdCuenta().equals(idCuenta);
    }

    public void verificarCuenta(Integer idCuenta) {
        if (!esAdministrador() && !esPropietario(idCuenta)) {
            throw new AccessDeniedException("No tienes acceso a esta cuenta.");
        }
    }

    public void verificarNegocio(Negocio negocio) {
        verificarCuenta(negocio.getCuenta().getIdCuenta());
    }

    public void verificarPedido(Pedido pedido) {
        verificarCuenta(pedido.getCuenta().getIdCuenta());
    }

    public boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
