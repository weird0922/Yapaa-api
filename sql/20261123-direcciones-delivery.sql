SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;

IF OBJECT_ID('dbo.direccion_cliente','U') IS NULL
CREATE TABLE dbo.direccion_cliente(id_direccion INT IDENTITY PRIMARY KEY,id_cuenta INT NOT NULL REFERENCES cuenta(id_cuenta),nombre NVARCHAR(40) NOT NULL,direccion NVARCHAR(250) NOT NULL,referencia NVARCHAR(120) NOT NULL DEFAULT '',latitud FLOAT NOT NULL,longitud FLOAT NOT NULL,predeterminada BIT NOT NULL DEFAULT 0,activa BIT NOT NULL DEFAULT 1,fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME());
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ux_direccion_predeterminada')
CREATE UNIQUE INDEX ux_direccion_predeterminada ON direccion_cliente(id_cuenta) WHERE predeterminada=1 AND activa=1;
IF OBJECT_ID('dbo.ubicacion_negocio','U') IS NULL
CREATE TABLE dbo.ubicacion_negocio(id_negocio INT PRIMARY KEY REFERENCES negocio(id_negocio),direccion NVARCHAR(250) NOT NULL,latitud FLOAT NOT NULL,longitud FLOAT NOT NULL,fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME());
IF OBJECT_ID('dbo.cotizacion_delivery','U') IS NULL
CREATE TABLE dbo.cotizacion_delivery(id VARCHAR(36) PRIMARY KEY,id_cuenta INT NOT NULL REFERENCES cuenta(id_cuenta),id_negocio INT NOT NULL REFERENCES negocio(id_negocio),id_direccion INT NOT NULL REFERENCES direccion_cliente(id_direccion),direccion NVARCHAR(400) NOT NULL,latitud FLOAT NOT NULL,longitud FLOAT NOT NULL,origen_latitud FLOAT NOT NULL,origen_longitud FLOAT NOT NULL,kilometros DECIMAL(8,3) NOT NULL,importe DECIMAL(10,2) NOT NULL,vence DATETIME2 NOT NULL,utilizada BIT NOT NULL DEFAULT 0);
