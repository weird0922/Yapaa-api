package pe.upn.yapaa_api.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.time.ZoneId;
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="yapaa.demo.habilitado",havingValue="true")
public class SimulacionConfig {

}
