package pe.upn.yapaa_api.controller;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import pe.upn.yapaa_api.common.Result;import pe.upn.yapaa_api.service.*;import java.util.*;
@RestController public class MapasController {
 private final MapasService mapas;private final DeliveryService delivery;public MapasController(MapasService m,DeliveryService d){mapas=m;delivery=d;}
 public record Cotizar(@NotNull @Positive Integer idNegocio,@NotNull @Positive Integer idDireccion){}
 public record Origen(@NotBlank @Size(max=36) String token){}
 @GetMapping("/api/mapas/buscar") public Result<List<MapasService.Punto>> buscar(@RequestParam String q){return Result.exitoso(mapas.buscar(q));}
 @GetMapping("/api/mapas/inversa") public Result<MapasService.Punto> inversa(@RequestParam double lat,@RequestParam double lon){return Result.exitoso(mapas.inversa(lat,lon));}
 @PostMapping("/api/delivery/cotizar") public Result<DeliveryService.Cotizacion> cotizar(@Valid @RequestBody Cotizar r){return Result.exitoso(delivery.cotizar(r.idNegocio(),r.idDireccion()));}
 @GetMapping("/api/negocios/{id}/ubicacion") public Result<DeliveryService.Ubicacion> origen(@PathVariable int id){return Result.exitoso(delivery.origen(id));}
 @PutMapping("/api/negocios/{id}/ubicacion") public Result<DeliveryService.Ubicacion> guardar(@PathVariable int id,@Valid @RequestBody Origen r){return Result.exitoso(delivery.guardarOrigen(id,r.token()));}
}
