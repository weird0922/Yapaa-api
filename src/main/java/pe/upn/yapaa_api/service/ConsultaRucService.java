package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pe.upn.yapaa_api.dto.negocio.ConsultaRucResponse;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.exception.ServicioExternoException;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class ConsultaRucService {
    private final RestClient cliente;
    private final String token;
    private final boolean demo;
    private final ConcurrentHashMap<String,Cache> cache=new ConcurrentHashMap<>();
    private record Cache(ConsultaRucResponse datos,long vence){}
    public record RespuestaProveedor(String razon_social,String numero_documento,String estado,String condicion){}
    public ConsultaRucService(@Qualifier("clienteRuc") RestClient cliente,
            @Value("${yapaa.ruc.token:}") String token,@Value("${yapaa.demo.habilitado:false}") boolean demo){
        this.cliente=cliente;this.token=token;this.demo=demo;
    }
    public ConsultaRucResponse consultar(String ruc){
        if(ruc==null || !ruc.matches("(10|20)[0-9]{9}"))throw new ReglaNegocioException("Ingresa un RUC de 11 digitos que empiece por 10 o 20.");
        Cache anterior=cache.get(ruc);
        if(anterior!=null && anterior.vence()>System.currentTimeMillis())return anterior.datos();
        if(demo && java.util.Set.of("20000000001","20000000002").contains(ruc)){
            return new ConsultaRucResponse(ruc,"NEGOCIO DE DEMOSTRACION YAPAA S.A.C.","ACTIVO","HABIDO",false,true);
        }
        if(token.isBlank())throw new ServicioExternoException("La consulta real de RUC requiere configurar la clave gratuita de Decolecta. Solo estan disponibles los RUC de demostracion indicados.");
        try{
            RespuestaProveedor datos=cliente.get().uri("/v1/sunat/ruc?numero={ruc}",ruc)
                    .headers(h->h.setBearerAuth(token)).retrieve().body(RespuestaProveedor.class);
            if(datos==null || datos.razon_social()==null || datos.razon_social().isBlank() || !ruc.equals(datos.numero_documento()))
                throw new ServicioExternoException("El proveedor no devolvio una razon social valida para ese RUC.");
            var resultado=new ConsultaRucResponse(ruc,datos.razon_social(),datos.estado(),datos.condicion(),true,false);
            if(cache.size()>=1000)cache.clear();
            cache.put(ruc,new Cache(resultado,System.currentTimeMillis()+24*60*60*1000L));
            return resultado;
        }catch(RestClientResponseException ex){
            int estado=ex.getStatusCode().value();
            if(estado==404 || estado==422)throw new ReglaNegocioException("No se encontro un contribuyente valido para ese RUC.");
            throw new ServicioExternoException(estado==429?"Se alcanzo el limite de consultas de RUC. Intenta mas tarde.":"La consulta de RUC no esta disponible. Revisa la configuracion del proveedor.");
        }catch(RestClientException ex){throw new ServicioExternoException("No fue posible consultar el RUC. Intenta nuevamente en unos instantes.");}
    }
}
