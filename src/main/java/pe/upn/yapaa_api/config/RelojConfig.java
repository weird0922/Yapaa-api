package pe.upn.yapaa_api.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;
@Configuration
public class RelojConfig {
    @Bean public Clock relojPeru(){return Clock.system(ZoneId.of("America/Lima"));}
}
