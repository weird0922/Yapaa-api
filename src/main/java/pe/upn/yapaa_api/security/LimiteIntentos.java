package pe.upn.yapaa_api.security;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
@Component
public class LimiteIntentos {
 private record Intento(Instant inicio,int numero){}
 private final Map<String,Intento> datos=new HashMap<>();private final Clock reloj;
 public LimiteIntentos(Clock r){reloj=r;}
 public synchronized void comprobar(String clave,int maximo){
  Instant t=reloj.instant();datos.entrySet().removeIf(e->e.getValue().inicio().plusSeconds(900).isBefore(t));
  if(datos.size()>=10000&&!datos.containsKey(clave))throw new ReglaNegocioException("Demasiadas solicitudes. Intenta mas tarde.");
  var i=datos.getOrDefault(clave,new Intento(t,0));if(i.numero()>=maximo)throw new ReglaNegocioException("Demasiados intentos. Espera 15 minutos.");datos.put(clave,new Intento(i.inicio(),i.numero()+1));
 }
}
