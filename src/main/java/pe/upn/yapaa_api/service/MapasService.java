package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import pe.upn.yapaa_api.exception.*;
import pe.upn.yapaa_api.security.*;
import java.time.*;import java.util.*;import java.util.concurrent.ConcurrentHashMap;
@Service public class MapasService {
 public record Punto(String token,String direccion,double latitud,double longitud){}
 public record Geometria(List<Double> coordinates){}
 public record Feature(Geometria geometry,Map<String,Object> properties){}
 public record Respuesta(List<Feature> features){}
 private record Cache(List<Punto> datos,long vence){}
 private record Permiso(Punto punto,Integer cuenta,long vence){}
 private final RestClient http;private final UsuarioActual usuario;private final LimiteIntentos limite;
 private final Map<String,Cache> cache=new ConcurrentHashMap<>();private final Map<String,Permiso> permisos=new ConcurrentHashMap<>();
 public MapasService(@Value("${yapaa.mapas.photon:https://photon.komoot.io}") String url,UsuarioActual u,LimiteIntentos l){var f=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());f.setReadTimeout(Duration.ofSeconds(8));http=RestClient.builder().requestFactory(f).baseUrl(url).defaultHeader("User-Agent","YAPAA/1.0 (yapaaservice@gmail.com)").build();usuario=u;limite=l;}
 public List<Punto> buscar(String texto){String q=texto==null?"":texto.strip();if(q.length()<3||q.length()>150)throw new ReglaNegocioException("Escribe entre 3 y 150 caracteres.");limite.comprobar("mapas:"+usuario.obtenerCorreo(),150);String clave=q.toLowerCase(Locale.ROOT);var anterior=cache.get(clave);List<Punto> datos;
 if(anterior!=null&&anterior.vence()>System.currentTimeMillis())datos=anterior.datos();else{
  Respuesta r;try{r=http.get().uri(b->b.path("/api/").queryParam("q",q).queryParam("limit",8).queryParam("bbox","-79.13,-8.22,-78.94,-7.98").queryParam("lat",-8.11).queryParam("lon",-79.03).build()).retrieve().body(Respuesta.class);}catch(Exception e){throw new ServicioExternoException("No pudimos buscar direcciones. Intenta nuevamente.");}
  datos=convertir(r);if(cache.size()>500)cache.clear();cache.put(clave,new Cache(datos,System.currentTimeMillis()+3600000));
 }return datos.stream().limit(6).map(p->autorizar(p.direccion(),p.latitud(),p.longitud())).toList();}
 public Punto inversa(double lat,double lon){CoberturaTrujillo.exigir(lat,lon);limite.comprobar("mapas:"+usuario.obtenerCorreo(),150);Respuesta r;
 try{r=http.get().uri(b->b.path("/reverse").queryParam("lat",lat).queryParam("lon",lon).queryParam("limit",5).build()).retrieve().body(Respuesta.class);}catch(Exception e){throw new ServicioExternoException("No pudimos identificar esa calle. Intenta nuevamente.");}
 var cercano=convertir(r).stream().filter(p->CoberturaTrujillo.metros(lat,lon,p.latitud(),p.longitud())<=250).findFirst().orElseThrow(()->new ReglaNegocioException("Acerca el marcador a una calle de Trujillo."));return autorizar(cercano.direccion(),lat,lon);}
 private List<Punto> convertir(Respuesta r){if(r==null||r.features()==null)return List.of();List<Punto> lista=new ArrayList<>();for(var x:r.features()){if(x.geometry()==null||x.geometry().coordinates()==null||x.geometry().coordinates().size()<2||x.properties()==null)continue;double lon=x.geometry().coordinates().get(0),lat=x.geometry().coordinates().get(1);if(!CoberturaTrujillo.contiene(lat,lon))continue;var p=x.properties();String calle=texto(p,"street");if(calle.isBlank())calle=texto(p,"name");String tipo=texto(p,"osm_value");if(calle.isBlank()||Set.of("city","state","country","county","town","village").contains(tipo))continue;String numero=texto(p,"housenumber"),zona=texto(p,"district");if(zona.isBlank())zona=texto(p,"city");String barrio=texto(p,"locality");String direccion=calle+(numero.isBlank()?"":" "+numero)+(barrio.isBlank()||barrio.equals(zona)?"":", "+barrio)+(zona.isBlank()?", Trujillo":", "+zona);if(direccion.length()<=250)lista.add(new Punto("",direccion,lat,lon));}return lista;}
 private String texto(Map<String,Object> p,String k){Object v=p.get(k);return v==null?"":v.toString();}
 private Punto autorizar(String direccion,double lat,double lon){long ahora=System.currentTimeMillis();permisos.entrySet().removeIf(e->e.getValue().vence()<ahora);if(permisos.size()>3000)throw new ServicioExternoException("Intenta de nuevo en unos minutos.");String token=UUID.randomUUID().toString();var punto=new Punto(token,direccion,lat,lon);permisos.put(token,new Permiso(punto,usuario.obtenerCuenta().getIdCuenta(),ahora+1200000));return punto;}
 public Punto validar(String token){var p=token==null?null:permisos.get(token);if(p==null||p.vence()<System.currentTimeMillis()||!p.cuenta().equals(usuario.obtenerCuenta().getIdCuenta()))throw new ReglaNegocioException("Vuelve a seleccionar la dirección en el mapa.");return p.punto();}
}
