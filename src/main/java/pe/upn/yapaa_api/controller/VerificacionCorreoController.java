package pe.upn.yapaa_api.controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import pe.upn.yapaa_api.service.VerificacionCorreoService;
import pe.upn.yapaa_api.security.LimiteIntentos;
import pe.upn.yapaa_api.common.Result;
@RestController @RequestMapping("/api/auth/correo")
public class VerificacionCorreoController {
 public record Solicitud(@NotBlank @Email @Size(max=254) String correo){}
 public record Verificar(@NotBlank @Email @Size(max=254) String correo,@NotBlank @Pattern(regexp="[0-9]{6}") String codigo){}
 public record Envio(boolean local,int segundosReenvio,int minutosVigencia){}
 public record Verificado(String tokenVerificacion){}
 private final VerificacionCorreoService servicio;private final LimiteIntentos limites;
 public VerificacionCorreoController(VerificacionCorreoService s,LimiteIntentos l){servicio=s;limites=l;}
 @PostMapping("/codigo") public Result<Envio> solicitar(@Valid @RequestBody Solicitud s,HttpServletRequest r){limites.comprobar("correo:"+r.getRemoteAddr(),20);return Result.exitoso(new Envio(servicio.solicitar(s.correo()),60,1));}
 @PostMapping("/verificar") public Result<Verificado> verificar(@Valid @RequestBody Verificar s,HttpServletRequest r){limites.comprobar("otp:"+r.getRemoteAddr(),50);return Result.exitoso(new Verificado(servicio.verificar(s.correo(),s.codigo())));}
}
