package pe.upn.yapaa_api.config;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import pe.upn.yapaa_api.service.PlanService;
@Component
public class PlanesIniciales {
    private final PlanService planes;
    public PlanesIniciales(PlanService planes){this.planes=planes;}
    @EventListener(ApplicationReadyEvent.class)
    public void preparar(){planes.prepararCatalogo();}
}
