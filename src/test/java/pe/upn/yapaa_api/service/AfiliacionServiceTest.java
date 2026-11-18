package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;
import pe.upn.yapaa_api.dto.negocio.AfiliacionRequest;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.security.UsuarioActual;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AfiliacionServiceTest {
    @Test void laRazonSocialProvieneDeLaConsultaYLaCuentaNoRecibeOtroRol(){
        var repo=mock(NegocioRepository.class);var cuentas=mock(CuentaRepository.class);
        var usuario=mock(UsuarioActual.class);var consulta=mock(ConsultaRucService.class);
        var servicio=new NegocioService(repo,cuentas,usuario,consulta,mock(PlanService.class));
        var cuenta=new Cuenta();cuenta.setIdCuenta(42);
        var rol=new Rol();rol.setNombre("NEGOCIO");cuenta.getRoles().add(rol);
        var solicitud=new AfiliacionRequest();solicitud.setRuc("20000000002");
        solicitud.setTipoContribuyente("PERSONA_JURIDICA");solicitud.setNombreComercial("Nombre del local");
        solicitud.setRazonSocial("VALOR MANIPULADO");
        when(repo.saveAndFlush(any())).thenAnswer(inv->{
            Negocio n=inv.getArgument(0);
            assertSame(cuenta,n.getCuenta());assertEquals("RAZON SOCIAL VERIFICADA",n.getRazonSocial());
            assertEquals("Nombre del local",n.getNombreComercial());assertEquals("PENDIENTE",n.getEstadoAprobacion());return n;
        });
        servicio.registrarNuevaCuenta(cuenta,solicitud,"RAZON SOCIAL VERIFICADA");
        assertEquals(1,cuenta.getRoles().size());
        assertFalse(cuenta.getRoles().stream().anyMatch(r->r.getNombre().equals("CLIENTE")));
    }
}
