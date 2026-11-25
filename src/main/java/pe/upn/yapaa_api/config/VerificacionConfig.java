package pe.upn.yapaa_api.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
@Configuration
public class VerificacionConfig {
 @Bean @Order(-1000) public CommandLineRunner crearTablaVerificacion(JdbcTemplate jdbc){return args->jdbc.execute("""
IF OBJECT_ID('dbo.verificacion_correo','U') IS NULL
CREATE TABLE dbo.verificacion_correo(
 correo NVARCHAR(254) NOT NULL PRIMARY KEY,
 codigo_hash VARCHAR(64) NOT NULL,
 sal VARCHAR(36) NOT NULL,
 vence DATETIME2 NOT NULL,
 enviado DATETIME2 NOT NULL,
 ventana DATETIME2 NOT NULL,
 envios INT NOT NULL,
 intentos INT NOT NULL,
 token_hash VARCHAR(64) NULL,
 token_vence DATETIME2 NULL,
 consumido BIT NOT NULL DEFAULT 0
);
""");}
}
