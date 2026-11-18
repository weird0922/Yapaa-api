package pe.upn.yapaa_api.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration
public class ConsultaRucConfig {
    @Bean("clienteRuc")
    public RestClient clienteRuc(){
        var fabrica=new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5000); fabrica.setReadTimeout(8000);
        return RestClient.builder().baseUrl("https://api.decolecta.com").requestFactory(fabrica).build();
    }
}
