package pe.upn.yapaa_api.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
@Configuration public class DireccionesConfig {
 @Bean @Order(-900) CommandLineRunner prepararDirecciones(JdbcTemplate jdbc,@org.springframework.beans.factory.annotation.Value("${yapaa.demo.habilitado:false}") boolean demo){return args->{jdbc.execute("""
IF OBJECT_ID('dbo.direccion_cliente','U') IS NULL
CREATE TABLE dbo.direccion_cliente(id_direccion INT IDENTITY PRIMARY KEY,id_cuenta INT NOT NULL REFERENCES cuenta(id_cuenta),nombre NVARCHAR(40) NOT NULL,direccion NVARCHAR(250) NOT NULL,referencia NVARCHAR(120) NOT NULL DEFAULT '',latitud FLOAT NOT NULL,longitud FLOAT NOT NULL,predeterminada BIT NOT NULL DEFAULT 0,activa BIT NOT NULL DEFAULT 1,fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME());
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ux_direccion_predeterminada')
CREATE UNIQUE INDEX ux_direccion_predeterminada ON direccion_cliente(id_cuenta) WHERE predeterminada=1 AND activa=1;
IF OBJECT_ID('dbo.ubicacion_negocio','U') IS NULL
CREATE TABLE dbo.ubicacion_negocio(id_negocio INT PRIMARY KEY REFERENCES negocio(id_negocio),direccion NVARCHAR(250) NOT NULL,latitud FLOAT NOT NULL,longitud FLOAT NOT NULL,fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME());
IF OBJECT_ID('dbo.cotizacion_delivery','U') IS NULL
CREATE TABLE dbo.cotizacion_delivery(id VARCHAR(36) PRIMARY KEY,id_cuenta INT NOT NULL REFERENCES cuenta(id_cuenta),id_negocio INT NOT NULL REFERENCES negocio(id_negocio),id_direccion INT NOT NULL REFERENCES direccion_cliente(id_direccion),direccion NVARCHAR(400) NOT NULL,latitud FLOAT NOT NULL,longitud FLOAT NOT NULL,origen_latitud FLOAT NOT NULL,origen_longitud FLOAT NOT NULL,kilometros DECIMAL(8,3) NOT NULL,importe DECIMAL(10,2) NOT NULL,vence DATETIME2 NOT NULL,utilizada BIT NOT NULL DEFAULT 0);
""");
 // Puntos ficticios solo para los negocios identificados del escenario local.
 if(demo)jdbc.update("INSERT INTO ubicacion_negocio(id_negocio,direccion,latitud,longitud) SELECT n.id_negocio,n.direccion,-8.105+(n.id_negocio%7)*0.003,-79.035+(n.id_negocio%5)*0.004 FROM negocio n WHERE (n.ruc LIKE '20999%' OR n.nombre_comercial IN ('La Burger Trujillana Demo','Pan Calientito Demo','Cafeteria La Esquina Demo')) AND NOT EXISTS(SELECT 1 FROM ubicacion_negocio u WHERE u.id_negocio=n.id_negocio)");
 };}
}
