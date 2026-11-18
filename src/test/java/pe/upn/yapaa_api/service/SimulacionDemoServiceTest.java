package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.model.Producto;
import pe.upn.yapaa_api.repository.ProductoRepository;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SimulacionDemoServiceTest {
    private final Clock reloj=Clock.fixed(Instant.parse("2026-11-17T17:00:00Z"),ZoneId.of("America/Lima"));
    private Producto producto(int id,String nombreNegocio,LocalDateTime limite){
        var n=new Negocio();n.setNombreComercial(nombreNegocio);
        var p=new Producto();p.setIdProducto(id);p.setNegocio(n);p.setStock(4);p.setActivo(true);
        p.setEstadoPublicacion("PUBLICADO");p.setFechaLimiteVenta(limite);p.setFechaRegistro(LocalDateTime.now(reloj));return p;
    }
    @Test void renuevaVencidosDeDemoSinTocarProductosReales(){
        var repo=mock(ProductoRepository.class);var ahora=LocalDateTime.now(reloj);
        var demo=producto(3,"La Burger Trujillana Demo",ahora.minusSeconds(1));
        var real=producto(100,"Negocio real",ahora.minusSeconds(1));
        when(repo.buscarSimulados(anyList(),anyList())).thenReturn(List.of(demo,real));
        new SimulacionDemoService(repo,reloj).actualizar();
        assertTrue(demo.getFechaLimiteVenta().isAfter(ahora));
        assertFalse(demo.getFechaLimiteVenta().isAfter(ahora.plusDays(2)));
        assertEquals(ahora.toLocalDate(),demo.getFechaPublicacion().toLocalDate());
        assertEquals(ahora.minusSeconds(1),real.getFechaLimiteVenta());
    }
    @Test void noReiniciaLaCuentaRegresivaEnCadaActualizacion(){
        var repo=mock(ProductoRepository.class);var ahora=LocalDateTime.now(reloj);
        var demo=producto(1,"La Burger Trujillana Demo",ahora.plusHours(4));
        when(repo.buscarSimulados(anyList(),anyList())).thenReturn(List.of(demo));
        new SimulacionDemoService(repo,reloj).actualizar();
        assertEquals(ahora.plusHours(4),demo.getFechaLimiteVenta());verify(repo,never()).saveAll(any());
    }
}
