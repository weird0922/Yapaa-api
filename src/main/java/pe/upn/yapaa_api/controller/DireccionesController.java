package pe.upn.yapaa_api.controller;
import org.springframework.web.bind.annotation.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import pe.upn.yapaa_api.common.Result;import pe.upn.yapaa_api.service.*;import java.util.*;
@RestController @RequestMapping("/api/direcciones") public class DireccionesController {
 private final DireccionesService servicio;public DireccionesController(DireccionesService s){servicio=s;}
 public record Guardar(@NotBlank @Size(max=40) String nombre,@Size(max=120) String referencia,@NotBlank @Size(max=36) String token,boolean predeterminada){}
 @GetMapping public Result<List<DireccionesService.Direccion>> listar(){return Result.exitoso(servicio.listar());}
 @PostMapping public Result<DireccionesService.Direccion> crear(@Valid @RequestBody Guardar r){return Result.exitoso(servicio.guardar(r.nombre(),r.referencia(),r.token(),r.predeterminada()));}
 @PatchMapping("/{id}/predeterminada") public Result<String> defecto(@PathVariable int id){servicio.predeterminada(id);return Result.exitoso("Dirección predeterminada actualizada.");}
 @DeleteMapping("/{id}") public Result<String> borrar(@PathVariable int id){servicio.eliminar(id);return Result.exitoso("Dirección eliminada.");}
}
