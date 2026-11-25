package pe.upn.yapaa_api.controller;
import org.springframework.web.bind.annotation.*;
import pe.upn.yapaa_api.service.DashboardService;
import pe.upn.yapaa_api.common.Result;
import java.time.*;
@io.swagger.v3.oas.annotations.Hidden
@RestController @RequestMapping("/api/dashboard")
public class DashboardController {
 private final DashboardService servicio;private final Clock reloj;
 @org.springframework.beans.factory.annotation.Autowired private pe.upn.yapaa_api.service.EscenarioDemoService escenario;
 public DashboardController(DashboardService s,Clock r){servicio=s;reloj=r;}
 @PostMapping("/demostracion") public Result<java.util.Map<String,Object>> demostracion(){return Result.exitoso(escenario.crear());}
 @GetMapping("/negocios/{id}") public Result<DashboardService.DetalleNegocio> detalle(@PathVariable Integer id,@org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) @RequestParam(required=false) LocalDate desde,@org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) @RequestParam(required=false) LocalDate hasta,@RequestParam(defaultValue="0") int pagina){LocalDate hoy=LocalDate.now(reloj);return Result.exitoso(servicio.detalle(id,desde==null?hoy.withDayOfMonth(1):desde,hasta==null?hoy:hasta,pagina));}
 @GetMapping public Result<DashboardService.Resumen> consultar(@org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) @RequestParam(required=false) LocalDate desde,@org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) @RequestParam(required=false) LocalDate hasta){LocalDate hoy=LocalDate.now(reloj);return Result.exitoso(servicio.consultar(desde==null?hoy.withDayOfMonth(1):desde,hasta==null?hoy:hasta));}
}
