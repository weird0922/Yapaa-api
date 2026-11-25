package pe.upn.yapaa_api.controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.service.PagoSimuladoService;
@RestController
public class PagoSimuladoController {
 public record Solicitud(@NotBlank @Pattern(regexp="APROBADO|RECHAZADO") String escenario){}
 private final PagoSimuladoService pagos;
 public PagoSimuladoController(PagoSimuladoService p){pagos=p;}
 @PostMapping("/api/pedidos/{id}/pago-simulado/anular") public Result<PagoSimuladoService.Respuesta> anular(@PathVariable Integer id){return Result.exitoso("Pedido cancelado.",pagos.anular(id));}
 @PostMapping("/api/pedidos/{id}/pago-simulado") public Result<PagoSimuladoService.Respuesta> simular(@PathVariable Integer id,@Valid @RequestBody Solicitud s){return Result.exitoso("Pago procesado.",pagos.simular(id,s.escenario()));}
}
