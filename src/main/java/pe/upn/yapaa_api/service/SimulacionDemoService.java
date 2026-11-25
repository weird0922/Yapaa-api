package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.repository.ProductoRepository;
import pe.upn.yapaa_api.model.Producto;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
@Service
@ConditionalOnProperty(name="yapaa.demo.habilitado",havingValue="true")
public class SimulacionDemoService {
    private static final List<String> NEGOCIOS=List.of("La Burger Trujillana Demo","Pan Calientito Demo","Cafeteria La Esquina Demo");
    private static final List<Integer> IDS=java.util.stream.IntStream.rangeClosed(1,22).boxed().toList();
    private final ProductoRepository productos;private final Clock reloj;
    public SimulacionDemoService(ProductoRepository productos,Clock reloj){this.productos=productos;this.reloj=reloj;}
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void iniciar(){actualizar();}
    @Scheduled(fixedDelayString="${yapaa.demo.intervalo-ms:30000}")
    @Transactional
    public void actualizar(){
        LocalDateTime ahora=LocalDateTime.now(reloj);
        List<Producto> cambios=new ArrayList<>();
        for(Producto p:productos.buscarSimulados(IDS,NEGOCIOS)){
            if(!IDS.contains(p.getIdProducto()) || p.getNegocio()==null || !NEGOCIOS.contains(p.getNegocio().getNombreComercial()))continue;
            boolean renovar=p.getFechaLimiteVenta()==null || !p.getFechaLimiteVenta().isAfter(ahora) || p.getFechaLimiteVenta().isAfter(ahora.plusDays(2));
            boolean fechaDeHoy=p.getFechaRegistro()!=null && p.getFechaRegistro().toLocalDate().equals(ahora.toLocalDate());
            if(renovar){
                int variante=p.getIdProducto()%3;
                LocalDateTime limite=variante==0?ahora.toLocalDate().atTime(LocalTime.of(23,59,59)):ahora.plusDays(variante);
                if(!limite.isAfter(ahora.plusMinutes(2)))limite=ahora.plusHours(1);
                p.setFechaLimiteVenta(limite);p.setFechaPublicacion(ahora);p.setFechaRegistro(ahora);
                if(p.getStock()<=0)p.setStock(5);
                p.setActivo(true);p.setEstadoPublicacion("PUBLICADO");
            }else if(!fechaDeHoy){p.setFechaRegistro(ahora);p.setFechaPublicacion(ahora);}
            if(renovar || !fechaDeHoy)cambios.add(p);
        }
        for(Producto p:productos.buscarEscenarioDemo()){
            if(p.getFechaLimiteVenta()==null||!p.getFechaLimiteVenta().isAfter(ahora)){p.setFechaLimiteVenta(ahora.plusHours(4+p.getIdProducto()%36));p.setFechaRegistro(ahora);p.setFechaPublicacion(ahora);if(p.getStock()<=0)p.setStock(8);cambios.add(p);}
        }
        if(!cambios.isEmpty())productos.saveAll(cambios);
    }
}
