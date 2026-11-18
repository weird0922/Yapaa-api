package pe.upn.yapaa_api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.model.Pedido;
import pe.upn.yapaa_api.repository.CuentaRepository;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioActualTest {
    private final CuentaRepository cuentas = mock(CuentaRepository.class);
    private final UsuarioActual usuario = new UsuarioActual(cuentas);
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    private void autenticar(String rol) {
        Cuenta cuenta = new Cuenta(); cuenta.setIdCuenta(10);
        when(cuentas.findByCorreo("usuario@test.invalid")).thenReturn(Optional.of(cuenta));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario@test.invalid", null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
    @Test void clientePuedeAccederASuCuentaPeroNoAOtra() {
        autenticar("CLIENTE");
        assertDoesNotThrow(() -> usuario.verificarCuenta(10));
        assertThrows(AccessDeniedException.class, () -> usuario.verificarCuenta(11));
    }
    @Test void negocioNoPuedeAccederAUnNegocioAjeno() {
        autenticar("NEGOCIO");
        Cuenta ajena = new Cuenta(); ajena.setIdCuenta(11);
        Negocio negocio = new Negocio(); negocio.setCuenta(ajena);
        assertThrows(AccessDeniedException.class, () -> usuario.verificarNegocio(negocio));
    }
    @Test void pedidoDeOtroClienteNoEsAccesible() {
        autenticar("CLIENTE");
        Cuenta ajena = new Cuenta(); ajena.setIdCuenta(11);
        Pedido pedido = new Pedido(); pedido.setCuenta(ajena);
        assertThrows(AccessDeniedException.class, () -> usuario.verificarPedido(pedido));
    }
    @Test void administradorPuedeRevisarRecursosDeOtrasCuentas() {
        autenticar("ADMIN");
        assertDoesNotThrow(() -> usuario.verificarCuenta(11));
    }
    @Test void visitanteNoTienePermisoSobreCuentas() {
        assertThrows(AccessDeniedException.class, () -> usuario.verificarCuenta(11));
    }
}
