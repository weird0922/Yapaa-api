package pe.upn.yapaa_api.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import pe.upn.yapaa_api.service.*;
import pe.upn.yapaa_api.dto.plan.*;
import pe.upn.yapaa_api.common.Result;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api")
public class PlanController {
    private final PlanService planes;private final VentaService ventas;
    public PlanController(PlanService p,VentaService v){planes=p;ventas=v;}
    public record CambioPlan(@NotBlank String nombre){}
    @GetMapping("/planes") public Result<List<PlanResponse>> planes(){return Result.exitoso(planes.disponibles());}
    @GetMapping("/negocios/{id}/plan") public Result<PlanResponse> plan(@PathVariable Integer id){return Result.exitoso(planes.consultar(id));}
    @PutMapping("/negocios/{id}/plan") public Result<PlanResponse> cambiar(@PathVariable Integer id,@Valid @RequestBody CambioPlan r){return Result.exitoso("Plan actualizado. Se aplicara a las ventas que confirmes desde ahora.",planes.cambiar(id,r.nombre()));}
    @GetMapping("/negocios/{id}/ventas") public Result<Page<VentaResponse>> ventas(@PathVariable Integer id,@PageableDefault(size=10,sort="idPedidoNegocio",direction=Sort.Direction.DESC) Pageable pagina){return Result.exitoso(ventas.listar(id,pagina));}
    @PostMapping("/ventas/{id}/confirmar") public Result<VentaResponse> confirmar(@PathVariable Integer id){return Result.exitoso("Venta realizada confirmada. Comision pendiente generada.",ventas.confirmar(id));}
    @PostMapping("/ventas/{id}/cancelar") public Result<VentaResponse> cancelar(@PathVariable Integer id){return Result.exitoso("Pedido cancelado. Stock devuelto.",ventas.cancelar(id));}
    @GetMapping("/negocios/{id}/indicadores") public Result<ResumenVentas> resumen(@PathVariable Integer id){return Result.exitoso(ventas.resumen(id));}
    @GetMapping("/negocios/{id}/reporte.csv") public ResponseEntity<String> csv(@PathVariable Integer id){return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).header("Content-Disposition","attachment; filename=ventas-"+id+".csv").body(ventas.csv(id));}
}
