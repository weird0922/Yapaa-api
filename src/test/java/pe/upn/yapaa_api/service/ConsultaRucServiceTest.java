package pe.upn.yapaa_api.service;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import pe.upn.yapaa_api.exception.ServicioExternoException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class ConsultaRucServiceTest {
    @Test void consultaRazonSocialConClaveEnCabeceraYReutilizaLaCache(){
        var builder=RestClient.builder().baseUrl("https://api.decolecta.com");
        var servidor=MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://api.decolecta.com/v1/sunat/ruc?numero=20601030013"))
                .andExpect(header("Authorization","Bearer clave-ficticia"))
                .andRespond(withSuccess("{\"numero_documento\":\"20601030013\",\"razon_social\":\"RAZON SOCIAL CONSULTADA S.A.C.\",\"estado\":\"ACTIVO\",\"condicion\":\"HABIDO\"}",MediaType.APPLICATION_JSON));
        var servicio=new ConsultaRucService(builder.build(),"clave-ficticia",false);
        var datos=servicio.consultar("20601030013");
        assertEquals("RAZON SOCIAL CONSULTADA S.A.C.",datos.razonSocial());
        assertTrue(datos.verificado());assertFalse(datos.demostracion());
        assertSame(datos,servicio.consultar("20601030013"));servidor.verify();
    }
    @Test void sinClaveNoSeInventaUnaValidacionReal(){
        var servicio=new ConsultaRucService(RestClient.create(),"",true);
        assertThrows(ServicioExternoException.class,()->servicio.consultar("20601030013"));
    }
    @Test void datosFicticiosSeIdentificanYNoExistenEnProduccion(){
        var local=new ConsultaRucService(RestClient.create(),"",true);
        var datos=local.consultar("20000000002");
        assertTrue(datos.demostracion());assertFalse(datos.verificado());
        var produccion=new ConsultaRucService(RestClient.create(),"",false);
        assertThrows(ServicioExternoException.class,()->produccion.consultar("20000000002"));
    }
    @Test void respuestaDeOtroRucSeRechaza(){
        var builder=RestClient.builder().baseUrl("https://api.decolecta.com");
        var servidor=MockRestServiceServer.bindTo(builder).build();
        servidor.expect(anything()).andRespond(withSuccess("{\"numero_documento\":\"20111111111\",\"razon_social\":\"OTRA EMPRESA\"}",MediaType.APPLICATION_JSON));
        var servicio=new ConsultaRucService(builder.build(),"ficticia",false);
        assertThrows(ServicioExternoException.class,()->servicio.consultar("20601030013"));
    }
}
