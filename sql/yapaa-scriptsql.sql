
/* ============================================================
   YAPAA_BD
   ============================================================ */

USE master;
GO

IF DB_ID(N'YAPAA_BD') IS NOT NULL
BEGIN
    ALTER DATABASE YAPAA_BD SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE YAPAA_BD;
END;
GO

CREATE DATABASE YAPAA_BD;
GO

USE YAPAA_BD;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

/* ============================================================
   1. CUENTAS, CLIENTES Y ROLES
   ============================================================ */

CREATE TABLE dbo.cuenta (
    id_cuenta INT IDENTITY(1,1) NOT NULL,
    correo NVARCHAR(254) NOT NULL,
    contrasena_hash NVARCHAR(255) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT ('ACTIVO'),
    fecha_registro DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    fecha_actualizacion DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_cuenta PRIMARY KEY (id_cuenta),
    CONSTRAINT UQ_cuenta_correo UNIQUE (correo),
    CONSTRAINT CK_cuenta_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);
GO

CREATE TABLE dbo.cliente (
    id_cuenta INT NOT NULL,
    nombres NVARCHAR(100) NOT NULL,
    apellidos NVARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    CONSTRAINT PK_cliente PRIMARY KEY (id_cuenta),
    CONSTRAINT FK_cliente_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES dbo.cuenta(id_cuenta),
    CONSTRAINT CK_cliente_nombres CHECK (LEN(LTRIM(RTRIM(nombres))) > 0),
    CONSTRAINT CK_cliente_apellidos CHECK (LEN(LTRIM(RTRIM(apellidos))) > 0),
    CONSTRAINT CK_cliente_telefono CHECK (LEN(LTRIM(RTRIM(telefono))) > 0)
);
GO

CREATE TABLE dbo.rol (
    id_rol INT IDENTITY(1,1) NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    descripcion NVARCHAR(200) NULL,
    CONSTRAINT PK_rol PRIMARY KEY (id_rol),
    CONSTRAINT UQ_rol_nombre UNIQUE (nombre),
    CONSTRAINT CK_rol_nombre CHECK (nombre IN ('CLIENTE','NEGOCIO','ADMIN'))
);
GO

CREATE TABLE dbo.cuenta_rol (
    id_cuenta INT NOT NULL,
    id_rol INT NOT NULL,
    CONSTRAINT PK_cuenta_rol PRIMARY KEY (id_cuenta,id_rol),
    CONSTRAINT FK_cuenta_rol_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES dbo.cuenta(id_cuenta),
    CONSTRAINT FK_cuenta_rol_rol FOREIGN KEY (id_rol)
        REFERENCES dbo.rol(id_rol)
);
GO

/* ============================================================
   2. NEGOCIOS Y PLANES COMERCIALES
   ============================================================ */

CREATE TABLE dbo.negocio (
    id_negocio INT IDENTITY(1,1) NOT NULL,
    id_cuenta INT NOT NULL,
    nombre_comercial NVARCHAR(150) NOT NULL,
    razon_social NVARCHAR(200) NULL,
    tipo_contribuyente VARCHAR(20) NOT NULL,
    ruc VARCHAR(11) NOT NULL,
    direccion NVARCHAR(250) NOT NULL,
    distrito NVARCHAR(100) NOT NULL,
    ciudad NVARCHAR(100) NOT NULL,
    telefono_contacto VARCHAR(20) NOT NULL,
    estado_aprobacion VARCHAR(12) NOT NULL DEFAULT ('PENDIENTE'),
    activo BIT NOT NULL DEFAULT (1),
    fecha_registro DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    fecha_actualizacion DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_negocio PRIMARY KEY (id_negocio),
    CONSTRAINT FK_negocio_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES dbo.cuenta(id_cuenta),
    CONSTRAINT CK_negocio_estado_aprobacion
        CHECK (estado_aprobacion IN ('PENDIENTE','APROBADO','RECHAZADO','SUSPENDIDO')),
    CONSTRAINT CK_negocio_tipo_contribuyente
        CHECK (tipo_contribuyente IN ('PERSONA_NATURAL','PERSONA_JURIDICA')),
    CONSTRAINT CK_negocio_ruc_formato
        CHECK (LEN(ruc)=11 AND ruc NOT LIKE '%[^0-9]%'
               AND ((tipo_contribuyente='PERSONA_NATURAL' AND ruc LIKE '10%')
                 OR (tipo_contribuyente='PERSONA_JURIDICA' AND ruc LIKE '20%'))),
    CONSTRAINT CK_negocio_nombre
        CHECK (LEN(LTRIM(RTRIM(nombre_comercial))) > 0),
    CONSTRAINT CK_negocio_direccion
        CHECK (LEN(LTRIM(RTRIM(direccion))) > 0),
    CONSTRAINT CK_negocio_distrito
        CHECK (LEN(LTRIM(RTRIM(distrito))) > 0),
    CONSTRAINT CK_negocio_ciudad
        CHECK (LEN(LTRIM(RTRIM(ciudad))) > 0),
    CONSTRAINT CK_negocio_telefono
        CHECK (LEN(LTRIM(RTRIM(telefono_contacto))) > 0)
);
GO

CREATE UNIQUE INDEX UX_negocio_ruc
ON dbo.negocio(ruc);
GO

CREATE INDEX IX_negocio_cuenta ON dbo.negocio(id_cuenta);
CREATE INDEX IX_negocio_publicacion ON dbo.negocio(estado_aprobacion,activo);
GO

CREATE TABLE dbo.plan_comercial (
    id_plan INT IDENTITY(1,1) NOT NULL,
    nombre VARCHAR(30) NOT NULL,
    porcentaje_comision DECIMAL(5,2) NOT NULL,
    descripcion NVARCHAR(250) NULL,
    activo BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_plan_comercial PRIMARY KEY (id_plan),
    CONSTRAINT UQ_plan_comercial_nombre UNIQUE (nombre),
    CONSTRAINT CK_plan_comercial_porcentaje
        CHECK (porcentaje_comision BETWEEN 0 AND 100)
);
GO

CREATE TABLE dbo.negocio_plan (
    id_negocio_plan INT IDENTITY(1,1) NOT NULL,
    id_negocio INT NOT NULL,
    id_plan INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NULL,
    porcentaje_aplicado DECIMAL(5,2) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT ('ACTIVO'),
    fecha_registro DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_negocio_plan PRIMARY KEY (id_negocio_plan),
    CONSTRAINT FK_negocio_plan_negocio FOREIGN KEY (id_negocio)
        REFERENCES dbo.negocio(id_negocio),
    CONSTRAINT FK_negocio_plan_plan FOREIGN KEY (id_plan)
        REFERENCES dbo.plan_comercial(id_plan),
    CONSTRAINT CK_negocio_plan_fechas
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT CK_negocio_plan_porcentaje
        CHECK (porcentaje_aplicado BETWEEN 0 AND 100),
    CONSTRAINT CK_negocio_plan_estado
        CHECK (estado IN ('ACTIVO','FINALIZADO','CANCELADO'))
);
GO

CREATE INDEX IX_negocio_plan_negocio_estado
ON dbo.negocio_plan(id_negocio,estado,fecha_inicio);
GO

/* ============================================================
   3. CATEGORIAS Y PRODUCTOS

   REGLA DE COMPRA (se implementa en la API Spring Boot):
   - Permitir comprar hasta un minuto antes de fecha_limite_venta.
   - La validacion debe repetirse al confirmar el pago.
   - Validar y descontar stock atomicamente en una transaccion.
   - El carrito no reserva stock.
   ============================================================ */

CREATE TABLE dbo.categoria (
    id_categoria INT IDENTITY(1,1) NOT NULL,
    nombre NVARCHAR(100) NOT NULL,
    descripcion NVARCHAR(250) NULL,
    activo BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT UQ_categoria_nombre UNIQUE (nombre),
    CONSTRAINT CK_categoria_nombre CHECK (LEN(LTRIM(RTRIM(nombre))) > 0)
);
GO

CREATE TABLE dbo.producto (
    id_producto INT IDENTITY(1,1) NOT NULL,
    id_negocio INT NOT NULL,
    id_categoria INT NOT NULL,
    nombre NVARCHAR(150) NOT NULL,
    descripcion NVARCHAR(500) NULL,
    imagen_url NVARCHAR(500) NULL,
    precio_normal DECIMAL(10,2) NOT NULL,
    precio_descuento DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    fecha_publicacion DATETIME2(0) NULL,
    fecha_limite_venta DATETIME2(0) NOT NULL,
    estado_publicacion VARCHAR(12) NOT NULL DEFAULT ('BORRADOR'),
    activo BIT NOT NULL DEFAULT (1),
    fecha_registro DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    fecha_actualizacion DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_producto PRIMARY KEY (id_producto),
    CONSTRAINT FK_producto_negocio FOREIGN KEY (id_negocio)
        REFERENCES dbo.negocio(id_negocio),
    CONSTRAINT FK_producto_categoria FOREIGN KEY (id_categoria)
        REFERENCES dbo.categoria(id_categoria),
    CONSTRAINT CK_producto_nombre CHECK (LEN(LTRIM(RTRIM(nombre))) > 0),
    CONSTRAINT CK_producto_precio_normal CHECK (precio_normal > 0),
    CONSTRAINT CK_producto_precio_descuento
        CHECK (precio_descuento > 0 AND precio_descuento < precio_normal),
    CONSTRAINT CK_producto_stock CHECK (stock >= 0),
    CONSTRAINT CK_producto_estado
        CHECK (estado_publicacion IN ('BORRADOR','PUBLICADO','AGOTADO','FINALIZADO'))
);
GO

CREATE INDEX IX_producto_negocio_estado
ON dbo.producto(id_negocio,estado_publicacion,activo);

CREATE INDEX IX_producto_categoria ON dbo.producto(id_categoria);
CREATE INDEX IX_producto_fecha_limite ON dbo.producto(fecha_limite_venta);
GO

/* Migracion para bases de datos ya creadas (ddl-auto=none):
   ALTER TABLE dbo.producto ADD imagen_url NVARCHAR(500) NULL;
*/
GO

/* ============================================================
   4. PEDIDOS, SUBPEDIDOS Y DETALLES
   ============================================================ */

CREATE TABLE dbo.pedido (
    id_pedido INT IDENTITY(1,1) NOT NULL,
    id_cuenta INT NOT NULL,
    codigo_pedido VARCHAR(30) NOT NULL,
    fecha_pedido DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    subtotal_productos DECIMAL(12,2) NOT NULL,
    costo_delivery_total DECIMAL(12,2) NOT NULL DEFAULT (0),
    total DECIMAL(12,2) NOT NULL,
    estado_pedido VARCHAR(15) NOT NULL DEFAULT ('PENDIENTE'),
    estado_pago VARCHAR(12) NOT NULL DEFAULT ('PENDIENTE'),
    stock_descontado BIT NULL,
    CONSTRAINT PK_pedido PRIMARY KEY (id_pedido),
    CONSTRAINT UQ_pedido_codigo UNIQUE (codigo_pedido),
    CONSTRAINT FK_pedido_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES dbo.cuenta(id_cuenta),
    CONSTRAINT CK_pedido_importes CHECK (
        subtotal_productos >= 0 AND costo_delivery_total >= 0 AND total >= 0
    ),
    CONSTRAINT CK_pedido_total
        CHECK (total = subtotal_productos + costo_delivery_total),
    CONSTRAINT CK_pedido_estado CHECK (estado_pedido IN (
        'PENDIENTE','CONFIRMADO','EN_PREPARACION','LISTO',
        'EN_CAMINO','COMPLETADO','CANCELADO'
    )),
    CONSTRAINT CK_pedido_estado_pago CHECK (estado_pago IN (
        'PENDIENTE','CONFIRMADO','RECHAZADO','ANULADO','REEMBOLSADO'
    ))
);
GO

CREATE INDEX IX_pedido_cuenta_fecha
ON dbo.pedido(id_cuenta,fecha_pedido DESC);

CREATE INDEX IX_pedido_estado_fecha
ON dbo.pedido(estado_pedido,fecha_pedido DESC);
GO

CREATE TABLE dbo.pedido_negocio (
    id_pedido_negocio INT IDENTITY(1,1) NOT NULL,
    id_pedido INT NOT NULL,
    id_negocio INT NOT NULL,
    modalidad_entrega VARCHAR(10) NOT NULL,
    direccion_entrega NVARCHAR(250) NULL,
    costo_delivery DECIMAL(10,2) NOT NULL DEFAULT (0),
    subtotal_productos DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT ('PENDIENTE'),
    fecha_recojo_programada DATETIME2(0) NULL,
    CONSTRAINT PK_pedido_negocio PRIMARY KEY (id_pedido_negocio),
    CONSTRAINT FK_pedido_negocio_pedido FOREIGN KEY (id_pedido)
        REFERENCES dbo.pedido(id_pedido),
    CONSTRAINT FK_pedido_negocio_negocio FOREIGN KEY (id_negocio)
        REFERENCES dbo.negocio(id_negocio),
    CONSTRAINT UQ_pedido_negocio UNIQUE (id_pedido,id_negocio),
    CONSTRAINT CK_pedido_negocio_modalidad
        CHECK (modalidad_entrega IN ('RECOJO','DELIVERY')),
    CONSTRAINT CK_pedido_negocio_direccion CHECK (
        modalidad_entrega = 'RECOJO'
        OR (modalidad_entrega = 'DELIVERY'
            AND direccion_entrega IS NOT NULL
            AND LEN(LTRIM(RTRIM(direccion_entrega))) > 0)
    ),
    CONSTRAINT CK_pedido_negocio_importes CHECK (
        costo_delivery >= 0 AND subtotal_productos >= 0 AND total >= 0
    ),
    CONSTRAINT CK_pedido_negocio_total
        CHECK (total = subtotal_productos + costo_delivery),
    CONSTRAINT CK_pedido_negocio_estado CHECK (estado IN (
        'PENDIENTE','CONFIRMADO','EN_PREPARACION','LISTO',
        'EN_CAMINO','COMPLETADO','CANCELADO'
    ))
);
GO

CREATE INDEX IX_pedido_negocio_negocio_estado
ON dbo.pedido_negocio(id_negocio,estado);
GO

CREATE TABLE dbo.detalle_pedido (
    id_detalle INT IDENTITY(1,1) NOT NULL,
    id_pedido_negocio INT NOT NULL,
    id_producto INT NOT NULL,
    nombre_producto NVARCHAR(150) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT PK_detalle_pedido PRIMARY KEY (id_detalle),
    CONSTRAINT FK_detalle_pedido_subpedido
        FOREIGN KEY (id_pedido_negocio)
        REFERENCES dbo.pedido_negocio(id_pedido_negocio),
    CONSTRAINT FK_detalle_pedido_producto
        FOREIGN KEY (id_producto)
        REFERENCES dbo.producto(id_producto),
    CONSTRAINT CK_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT CK_detalle_precio CHECK (precio_unitario > 0),
    CONSTRAINT CK_detalle_subtotal
        CHECK (subtotal >= 0 AND subtotal = cantidad * precio_unitario)
);
GO

CREATE INDEX IX_detalle_pedido_subpedido
ON dbo.detalle_pedido(id_pedido_negocio);

CREATE INDEX IX_detalle_pedido_producto
ON dbo.detalle_pedido(id_producto);
GO

/* ============================================================
   5. PAGOS
   ============================================================ */

CREATE TABLE dbo.pago (
    id_pago INT IDENTITY(1,1) NOT NULL,
    id_pedido INT NOT NULL,
    id_cuenta_registro INT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    estado_pago VARCHAR(12) NOT NULL DEFAULT ('PENDIENTE'),
    referencia NVARCHAR(100) NULL,
    fecha_pago DATETIME2(0) NULL,
    fecha_registro DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_pago PRIMARY KEY (id_pago),
    CONSTRAINT FK_pago_pedido FOREIGN KEY (id_pedido)
        REFERENCES dbo.pedido(id_pedido),
    CONSTRAINT FK_pago_cuenta FOREIGN KEY (id_cuenta_registro)
        REFERENCES dbo.cuenta(id_cuenta),
    CONSTRAINT CK_pago_metodo CHECK (metodo_pago IN (
        'EFECTIVO','TRANSFERENCIA','YAPE','PLIN','TARJETA','OTRO'
    )),
    CONSTRAINT CK_pago_monto CHECK (monto > 0),
    CONSTRAINT CK_pago_estado CHECK (estado_pago IN (
        'PENDIENTE','CONFIRMADO','RECHAZADO','ANULADO'
    ))
);
GO

CREATE INDEX IX_pago_pedido_estado ON dbo.pago(id_pedido,estado_pago);
CREATE INDEX IX_pago_fecha_registro ON dbo.pago(fecha_registro DESC);
GO

/* ============================================================
   6. COMISIONES
   ============================================================ */

CREATE TABLE dbo.comision (
    id_comision INT IDENTITY(1,1) NOT NULL,
    id_pedido_negocio INT NOT NULL,
    base_calculo DECIMAL(12,2) NOT NULL,
    porcentaje_aplicado DECIMAL(5,2) NOT NULL,
    monto_comision DECIMAL(12,2) NOT NULL,
    estado VARCHAR(12) NOT NULL DEFAULT ('PENDIENTE'),
    fecha_liquidacion DATETIME2(0) NULL,
    fecha_registro DATETIME2(0) NOT NULL DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_comision PRIMARY KEY (id_comision),
    CONSTRAINT FK_comision_subpedido FOREIGN KEY (id_pedido_negocio)
        REFERENCES dbo.pedido_negocio(id_pedido_negocio),
    CONSTRAINT UQ_comision_subpedido UNIQUE (id_pedido_negocio),
    CONSTRAINT CK_comision_base CHECK (base_calculo >= 0),
    CONSTRAINT CK_comision_porcentaje
        CHECK (porcentaje_aplicado BETWEEN 0 AND 100),
    CONSTRAINT CK_comision_monto CHECK (monto_comision >= 0),
    CONSTRAINT CK_comision_estado
        CHECK (estado IN ('PENDIENTE','LIQUIDADA','ANULADA'))
);
GO

CREATE INDEX IX_comision_estado_fecha
ON dbo.comision(estado,fecha_registro DESC);
GO

/* ============================================================
   7. DATOS INICIALES
   ============================================================ */

INSERT INTO dbo.rol(nombre,descripcion)
VALUES
('CLIENTE',N'Puede realizar compras en YAPAA.'),
('NEGOCIO',N'Puede administrar negocios registrados.'),
('ADMIN',N'Administra y supervisa la plataforma.');

INSERT INTO dbo.plan_comercial(nombre,porcentaje_comision,descripcion)
VALUES
('Promocional',10.00,N'Comision promocional inicial.'),
('Normal',15.00,N'Plan comercial estandar.'),
('Pro',20.00,N'Plan comercial avanzado.');

INSERT INTO dbo.categoria(nombre,descripcion)
VALUES
(N'Hamburguesas',N'Hamburguesas clasicas y especiales.'),
(N'Comida rapida',N'Salchipapas, alitas y piqueos.'),
(N'Sandwiches',N'Sandwiches, triples y croissants rellenos.'),
(N'Panaderia y pasteleria',N'Empanadas, panes y postres.');
GO

/* ============================================================
   8. CUENTAS Y CLIENTES DE PRUEBA
   Contraseñas de muestra: no son hashes reales.
   ============================================================ */

INSERT INTO dbo.cuenta(correo,contrasena_hash)
VALUES
(N'cesar.cliente@yapaa.test',N'HASH_DE_PRUEBA_001'),
(N'ana.cliente@yapaa.test',N'HASH_DE_PRUEBA_002'),
(N'marco.cliente@yapaa.test',N'HASH_DE_PRUEBA_003'),
(N'burger.demo@yapaa.test',N'HASH_DE_PRUEBA_004'),
(N'panaderia.demo@yapaa.test',N'HASH_DE_PRUEBA_005'),
(N'cafeteria.demo@yapaa.test',N'HASH_DE_PRUEBA_006'),
(N'admin@yapaa.test',N'HASH_DE_PRUEBA_007');
GO

INSERT INTO dbo.cliente(id_cuenta,nombres,apellidos,telefono)
SELECT id_cuenta,N'Cesar',N'Arias Blas','987654321'
FROM dbo.cuenta WHERE correo=N'cesar.cliente@yapaa.test';

INSERT INTO dbo.cliente(id_cuenta,nombres,apellidos,telefono)
SELECT id_cuenta,N'Ana',N'Torres Ruiz','986123456'
FROM dbo.cuenta WHERE correo=N'ana.cliente@yapaa.test';

INSERT INTO dbo.cliente(id_cuenta,nombres,apellidos,telefono)
SELECT id_cuenta,N'Marco',N'Vasquez Leon','985123456'
FROM dbo.cuenta WHERE correo=N'marco.cliente@yapaa.test';
GO

INSERT INTO dbo.cuenta_rol(id_cuenta,id_rol)
SELECT c.id_cuenta,r.id_rol
FROM dbo.cuenta c
CROSS JOIN dbo.rol r
WHERE
    (c.correo IN (
        N'cesar.cliente@yapaa.test',
        N'ana.cliente@yapaa.test',
        N'marco.cliente@yapaa.test'
    ) AND r.nombre='CLIENTE')
 OR (c.correo IN (
        N'burger.demo@yapaa.test',
        N'panaderia.demo@yapaa.test',
        N'cafeteria.demo@yapaa.test'
    ) AND r.nombre='NEGOCIO')
 OR (c.correo=N'admin@yapaa.test' AND r.nombre='ADMIN');
GO

/* ============================================================
   9. NEGOCIOS DE DEMOSTRACION EN TRUJILLO
   Son negocios ficticios, no establecimientos oficiales.
   Los RUC son valores de prueba de formato; NO estan verificados por SUNAT.
   ============================================================ */

INSERT INTO dbo.negocio(
    id_cuenta,nombre_comercial,razon_social,tipo_contribuyente,ruc,
    direccion,distrito,ciudad,telefono_contacto,
    estado_aprobacion,activo
)
SELECT id_cuenta,N'La Burger Trujillana Demo',
    N'La Burger Trujillana Demo S.A.C.','PERSONA_JURIDICA','20123456789',
    N'Av. America Oeste 100',N'Trujillo',N'Trujillo',
    '984111222','APROBADO',1
FROM dbo.cuenta WHERE correo=N'burger.demo@yapaa.test';

INSERT INTO dbo.negocio(
    id_cuenta,nombre_comercial,razon_social,tipo_contribuyente,ruc,
    direccion,distrito,ciudad,telefono_contacto,
    estado_aprobacion,activo
)
SELECT id_cuenta,N'Pan Calientito Demo',
    NULL,'PERSONA_NATURAL','10123456789',
    N'Jiron Pizarro 250',N'Trujillo',N'Trujillo',
    '984222333','APROBADO',1
FROM dbo.cuenta WHERE correo=N'panaderia.demo@yapaa.test';

INSERT INTO dbo.negocio(
    id_cuenta,nombre_comercial,razon_social,tipo_contribuyente,ruc,
    direccion,distrito,ciudad,telefono_contacto,
    estado_aprobacion,activo
)
SELECT id_cuenta,N'Cafeteria La Esquina Demo',
    N'Cafeteria La Esquina Demo S.A.C.','PERSONA_JURIDICA','20123456780',
    N'Av. Fatima 300',N'Victor Larco Herrera',N'Trujillo',
    '984333444','APROBADO',1
FROM dbo.cuenta WHERE correo=N'cafeteria.demo@yapaa.test';
GO

/* ============================================================
   10. PLANES ASIGNADOS
   ============================================================ */

INSERT INTO dbo.negocio_plan(
    id_negocio,id_plan,fecha_inicio,fecha_fin,
    porcentaje_aplicado,estado
)
SELECT n.id_negocio,p.id_plan,CAST(GETDATE() AS DATE),
       NULL,p.porcentaje_comision,'ACTIVO'
FROM dbo.negocio n
INNER JOIN dbo.plan_comercial p
    ON p.nombre =
       CASE n.nombre_comercial
           WHEN N'La Burger Trujillana Demo' THEN 'Promocional'
           WHEN N'Pan Calientito Demo' THEN 'Normal'
           WHEN N'Cafeteria La Esquina Demo' THEN 'Pro'
       END;
GO

/* ============================================================
   11. PRODUCTOS DE PRUEBA
   Importes de ejemplo en soles. No se incluyen bebidas.
   ============================================================ */

DECLARE @Burger INT =
(SELECT id_negocio FROM dbo.negocio
 WHERE nombre_comercial=N'La Burger Trujillana Demo');

DECLARE @Panaderia INT =
(SELECT id_negocio FROM dbo.negocio
 WHERE nombre_comercial=N'Pan Calientito Demo');

DECLARE @Cafe INT =
(SELECT id_negocio FROM dbo.negocio
 WHERE nombre_comercial=N'Cafeteria La Esquina Demo');

DECLARE @CatBurger INT =
(SELECT id_categoria FROM dbo.categoria WHERE nombre=N'Hamburguesas');

DECLARE @CatRapida INT =
(SELECT id_categoria FROM dbo.categoria WHERE nombre=N'Comida rapida');

DECLARE @CatSandwich INT =
(SELECT id_categoria FROM dbo.categoria WHERE nombre=N'Sandwiches');

DECLARE @CatPan INT =
(SELECT id_categoria FROM dbo.categoria WHERE nombre=N'Panaderia y pasteleria');

INSERT INTO dbo.producto(
    id_negocio,id_categoria,nombre,descripcion,
    precio_normal,precio_descuento,stock,
    fecha_publicacion,fecha_limite_venta,estado_publicacion,activo
)
VALUES
(@Burger,@CatBurger,N'Hamburguesa Clasica',N'Carne, lechuga, tomate y salsas.',15.00,9.90,8,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatBurger,N'Cheeseburger',N'Hamburguesa con queso cheddar.',18.00,11.90,6,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatBurger,N'Hamburguesa Royal',N'Carne, queso y huevo.',21.00,13.90,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatBurger,N'Hamburguesa Doble',N'Doble carne y queso.',25.00,16.90,4,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatBurger,N'Hamburguesa BBQ',N'Carne, queso y salsa BBQ.',20.00,12.90,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatRapida,N'Salchipapa Clasica',N'Papas fritas, salchicha y salsas.',14.00,8.90,7,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatRapida,N'Salchipapa Especial',N'Papas fritas, salchicha y huevo.',18.00,11.90,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatRapida,N'Alitas BBQ x 6',N'Seis alitas con salsa barbacoa.',22.00,14.90,4,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatRapida,N'Alitas Crocantes x 6',N'Alitas crocantes con salsa de la casa.',21.00,13.90,4,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatSandwich,N'Sandwich de Pollo',N'Pan con pollo y verduras.',12.00,7.90,6,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Burger,@CatSandwich,N'Sandwich Mixto',N'Pan tostado con jamon y queso.',10.00,6.90,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Empanada de Pollo',N'Empanada horneada rellena de pollo.',6.00,3.90,12,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Empanada de Carne',N'Empanada horneada rellena de carne.',6.50,4.20,10,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Croissant Clasico',N'Croissant de masa hojaldrada.',7.00,4.50,8,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Croissant de Jamon y Queso',N'Croissant relleno de jamon y queso.',10.00,6.90,6,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Brownie de Chocolate',N'Brownie de chocolate.',7.00,4.50,10,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Alfajor Artesanal',N'Alfajor relleno de manjar blanco.',4.00,2.50,12,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Panaderia,@CatPan,N'Pan con Pollo',N'Pan relleno con pollo y mayonesa.',9.00,5.90,8,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Cafe,@CatSandwich,N'Club Sandwich',N'Pan, pollo, jamon, queso, huevo y verduras.',19.00,12.90,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Cafe,@CatSandwich,N'Sandwich de Pollo Crocante',N'Pollo crocante, lechuga y tomate.',16.00,10.90,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Cafe,@CatPan,N'Torta de Chocolate',N'Porcion individual de torta de chocolate.',12.00,7.90,6,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1),
(@Cafe,@CatPan,N'Cheesecake Individual',N'Porcion individual de cheesecake.',13.00,8.50,5,SYSDATETIME(),DATEADD(DAY,1,SYSDATETIME()),'PUBLICADO',1);
GO

/* Fotografias locales de demostracion. No reemplazar imagenes propias de los negocios. */
UPDATE dbo.producto SET imagen_url = CASE
    WHEN nombre LIKE N'Hamburguesa%' OR nombre = N'Cheeseburger' THEN '/imagenes/productos/hamburguesa.jpg'
    WHEN nombre LIKE N'Salchipapa%' THEN '/imagenes/productos/papas.jpg'
    WHEN nombre LIKE N'Alitas%' THEN '/imagenes/productos/alitas.jpg'
    WHEN nombre LIKE N'Empanada%' THEN '/imagenes/productos/empanada.jpg'
    WHEN nombre = N'Croissant Clasico' THEN '/imagenes/productos/croissant.jpg'
    WHEN nombre LIKE N'Croissant%' THEN '/imagenes/productos/croissant-relleno.jpg'
    WHEN nombre LIKE N'Brownie%' THEN '/imagenes/productos/brownie.jpg'
    WHEN nombre LIKE N'Alfajor%' THEN '/imagenes/productos/alfajor.jpg'
    WHEN nombre LIKE N'Torta%' THEN '/imagenes/productos/torta.jpg'
    WHEN nombre LIKE N'Cheesecake%' THEN '/imagenes/productos/cheesecake.jpg'
    WHEN nombre LIKE N'Sandwich%' OR nombre LIKE N'Club Sandwich%' OR nombre LIKE N'Pan con Pollo%' THEN '/imagenes/productos/sandwich.jpg'
    ELSE imagen_url
END WHERE imagen_url IS NULL OR imagen_url LIKE 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?%';
GO

/* ============================================================
   12. PEDIDOS DE PRUEBA
   ============================================================ */

DECLARE @Cliente1 INT =
(SELECT id_cuenta FROM dbo.cuenta WHERE correo=N'cesar.cliente@yapaa.test');

DECLARE @Cliente2 INT =
(SELECT id_cuenta FROM dbo.cuenta WHERE correo=N'ana.cliente@yapaa.test');

INSERT INTO dbo.pedido(
    id_cuenta,codigo_pedido,subtotal_productos,
    costo_delivery_total,total,estado_pedido,estado_pago
)
VALUES
(@Cliente1,'YAP-TEST-0001',19.80,4.00,23.80,'COMPLETADO','CONFIRMADO'),
(@Cliente2,'YAP-TEST-0002',7.80,0.00,7.80,'LISTO','PENDIENTE'),
(@Cliente1,'YAP-TEST-0003',11.90,3.00,14.90,'PENDIENTE','PENDIENTE');
GO

/* ============================================================
   13. SUBPEDIDOS
   ============================================================ */

DECLARE @Pedido1 INT =
(SELECT id_pedido FROM dbo.pedido WHERE codigo_pedido='YAP-TEST-0001');

DECLARE @Pedido2 INT =
(SELECT id_pedido FROM dbo.pedido WHERE codigo_pedido='YAP-TEST-0002');

DECLARE @Pedido3 INT =
(SELECT id_pedido FROM dbo.pedido WHERE codigo_pedido='YAP-TEST-0003');

DECLARE @NegBurger INT =
(SELECT id_negocio FROM dbo.negocio
 WHERE nombre_comercial=N'La Burger Trujillana Demo');

DECLARE @NegPan INT =
(SELECT id_negocio FROM dbo.negocio
 WHERE nombre_comercial=N'Pan Calientito Demo');

INSERT INTO dbo.pedido_negocio(
    id_pedido,id_negocio,modalidad_entrega,direccion_entrega,
    costo_delivery,subtotal_productos,total,estado
)
VALUES
(@Pedido1,@NegBurger,'DELIVERY',N'Av. America Oeste 500, Trujillo',4.00,19.80,23.80,'COMPLETADO'),
(@Pedido2,@NegPan,'RECOJO',NULL,0.00,7.80,7.80,'LISTO'),
(@Pedido3,@NegBurger,'DELIVERY',N'Jiron Los Laureles 120, Trujillo',3.00,11.90,14.90,'PENDIENTE');
GO

/* ============================================================
   14. DETALLES DE PEDIDOS
   ============================================================ */

INSERT INTO dbo.detalle_pedido(
    id_pedido_negocio,id_producto,nombre_producto,
    cantidad,precio_unitario,subtotal
)
SELECT pn.id_pedido_negocio,p.id_producto,p.nombre,2,9.90,19.80
FROM dbo.pedido_negocio pn
INNER JOIN dbo.pedido pe ON pe.id_pedido=pn.id_pedido
INNER JOIN dbo.producto p
    ON p.id_negocio=pn.id_negocio
   AND p.nombre=N'Hamburguesa Clasica'
WHERE pe.codigo_pedido='YAP-TEST-0001';

INSERT INTO dbo.detalle_pedido(
    id_pedido_negocio,id_producto,nombre_producto,
    cantidad,precio_unitario,subtotal
)
SELECT pn.id_pedido_negocio,p.id_producto,p.nombre,2,3.90,7.80
FROM dbo.pedido_negocio pn
INNER JOIN dbo.pedido pe ON pe.id_pedido=pn.id_pedido
INNER JOIN dbo.producto p
    ON p.id_negocio=pn.id_negocio
   AND p.nombre=N'Empanada de Pollo'
WHERE pe.codigo_pedido='YAP-TEST-0002';

INSERT INTO dbo.detalle_pedido(
    id_pedido_negocio,id_producto,nombre_producto,
    cantidad,precio_unitario,subtotal
)
SELECT pn.id_pedido_negocio,p.id_producto,p.nombre,1,11.90,11.90
FROM dbo.pedido_negocio pn
INNER JOIN dbo.pedido pe ON pe.id_pedido=pn.id_pedido
INNER JOIN dbo.producto p
    ON p.id_negocio=pn.id_negocio
   AND p.nombre=N'Cheeseburger'
WHERE pe.codigo_pedido='YAP-TEST-0003';
GO

/* ============================================================
   15. PAGOS
   ============================================================ */

INSERT INTO dbo.pago(
    id_pedido,metodo_pago,monto,estado_pago,referencia,fecha_pago
)
SELECT id_pedido,'YAPE',23.80,'CONFIRMADO',
       N'YAP-PAGO-TEST-0001',SYSDATETIME()
FROM dbo.pedido WHERE codigo_pedido='YAP-TEST-0001';

INSERT INTO dbo.pago(
    id_pedido,metodo_pago,monto,estado_pago,referencia
)
SELECT id_pedido,'EFECTIVO',7.80,'PENDIENTE',N'YAP-PAGO-TEST-0002'
FROM dbo.pedido WHERE codigo_pedido='YAP-TEST-0002';

INSERT INTO dbo.pago(
    id_pedido,metodo_pago,monto,estado_pago,referencia
)
SELECT id_pedido,'PLIN',14.90,'PENDIENTE',N'YAP-PAGO-TEST-0003'
FROM dbo.pedido WHERE codigo_pedido='YAP-TEST-0003';
GO

/* ============================================================
   16. COMISIONES
   La base excluye el delivery.
   ============================================================ */

INSERT INTO dbo.comision(
    id_pedido_negocio,base_calculo,porcentaje_aplicado,
    monto_comision,estado,fecha_liquidacion
)
SELECT
    pn.id_pedido_negocio,
    pn.subtotal_productos,
    np.porcentaje_aplicado,
    ROUND(pn.subtotal_productos*np.porcentaje_aplicado/100.0,2),
    CASE WHEN pe.codigo_pedido='YAP-TEST-0001'
         THEN 'LIQUIDADA' ELSE 'PENDIENTE' END,
    CASE WHEN pe.codigo_pedido='YAP-TEST-0001'
         THEN SYSDATETIME() ELSE NULL END
FROM dbo.pedido_negocio pn
INNER JOIN dbo.pedido pe ON pe.id_pedido=pn.id_pedido
INNER JOIN dbo.negocio_plan np
    ON np.id_negocio=pn.id_negocio
   AND np.estado='ACTIVO'
WHERE pe.codigo_pedido IN (
    'YAP-TEST-0001','YAP-TEST-0002','YAP-TEST-0003'
);
GO

/* ============================================================
   17. VER TODAS LAS TABLAS
   ============================================================ */

SELECT * FROM dbo.cuenta;
SELECT * FROM dbo.cliente;
SELECT * FROM dbo.rol;
SELECT * FROM dbo.cuenta_rol;
SELECT * FROM dbo.negocio;
SELECT * FROM dbo.plan_comercial;
SELECT * FROM dbo.negocio_plan;
SELECT * FROM dbo.categoria;
SELECT * FROM dbo.producto;
SELECT * FROM dbo.pedido;
SELECT * FROM dbo.pedido_negocio;
SELECT * FROM dbo.detalle_pedido;
SELECT * FROM dbo.pago;
SELECT * FROM dbo.comision;
GO

/* ============================================================
   18. CUENTAS Y ROLES
   ============================================================ */

SELECT c.id_cuenta,c.correo,c.estado,r.nombre AS rol
FROM dbo.cuenta c
LEFT JOIN dbo.cuenta_rol cr ON cr.id_cuenta=c.id_cuenta
LEFT JOIN dbo.rol r ON r.id_rol=cr.id_rol
ORDER BY c.id_cuenta;
GO

/* ============================================================
   19. NEGOCIOS Y PLANES COMERCIALES
   Alias corregido: nombre_plan.
   ============================================================ */

SELECT
    n.id_negocio,
    n.nombre_comercial,
    n.distrito,
    n.ciudad,
    n.estado_aprobacion,
    n.activo,
    c.correo AS correo_propietario,
    pc.nombre AS nombre_plan,
    np.porcentaje_aplicado
FROM dbo.negocio AS n
INNER JOIN dbo.cuenta AS c ON c.id_cuenta=n.id_cuenta
LEFT JOIN dbo.negocio_plan AS np
    ON np.id_negocio=n.id_negocio AND np.estado='ACTIVO'
LEFT JOIN dbo.plan_comercial AS pc ON pc.id_plan=np.id_plan
ORDER BY n.nombre_comercial;
GO

/* ============================================================
   20. PRODUCTOS, CATEGORIAS Y NEGOCIOS
   ============================================================ */

SELECT
    p.id_producto,
    p.nombre AS producto,
    cat.nombre AS categoria,
    n.nombre_comercial AS negocio,
    p.precio_normal,
    p.precio_descuento,
    CAST((p.precio_normal-p.precio_descuento)*100.0/
         NULLIF(p.precio_normal,0) AS DECIMAL(5,2)) AS descuento_porcentaje,
    p.stock,
    p.fecha_limite_venta,
    p.estado_publicacion
FROM dbo.producto p
INNER JOIN dbo.categoria cat ON cat.id_categoria=p.id_categoria
INNER JOIN dbo.negocio n ON n.id_negocio=p.id_negocio
ORDER BY cat.nombre,p.nombre;
GO

/* ============================================================
   21. PRODUCTOS DISPONIBLES
   ============================================================ */

SELECT
    p.id_producto,p.nombre,p.descripcion,
    n.nombre_comercial AS negocio,n.distrito,
    p.precio_normal,p.precio_descuento,p.stock,p.fecha_limite_venta
FROM dbo.producto p
INNER JOIN dbo.negocio n ON n.id_negocio=p.id_negocio
WHERE p.estado_publicacion='PUBLICADO'
  AND p.activo=1
  AND p.stock>0
  AND p.fecha_limite_venta>=DATEADD(MINUTE,1,SYSDATETIME())
  AND n.estado_aprobacion='APROBADO'
  AND n.activo=1
ORDER BY p.fecha_limite_venta;
GO

/* ============================================================
   22. PEDIDOS Y CLIENTES
   ============================================================ */

SELECT
    pe.id_pedido,pe.codigo_pedido,pe.fecha_pedido,
    cl.nombres,cl.apellidos,c.correo,
    pe.subtotal_productos,pe.costo_delivery_total,
    pe.total,pe.estado_pedido,pe.estado_pago
FROM dbo.pedido pe
INNER JOIN dbo.cuenta c ON c.id_cuenta=pe.id_cuenta
LEFT JOIN dbo.cliente cl ON cl.id_cuenta=c.id_cuenta
ORDER BY pe.fecha_pedido DESC;
GO

/* ============================================================
   23. DETALLES DE PEDIDOS
   ============================================================ */

SELECT
    pe.codigo_pedido,n.nombre_comercial AS negocio,
    d.nombre_producto,d.cantidad,d.precio_unitario,d.subtotal
FROM dbo.detalle_pedido d
INNER JOIN dbo.pedido_negocio pn
    ON pn.id_pedido_negocio=d.id_pedido_negocio
INNER JOIN dbo.pedido pe ON pe.id_pedido=pn.id_pedido
INNER JOIN dbo.negocio n ON n.id_negocio=pn.id_negocio
ORDER BY pe.fecha_pedido DESC;
GO

/* ============================================================
   24. PAGOS
   ============================================================ */

SELECT
    pa.id_pago,pe.codigo_pedido,c.correo AS cliente,
    pa.metodo_pago,pa.monto,pa.estado_pago,
    pa.referencia,pa.fecha_pago,pa.fecha_registro
FROM dbo.pago pa
INNER JOIN dbo.pedido pe ON pe.id_pedido=pa.id_pedido
INNER JOIN dbo.cuenta c ON c.id_cuenta=pe.id_cuenta
ORDER BY pa.fecha_registro DESC;
GO

/* ============================================================
   25. COMISIONES
   ============================================================ */

SELECT
    co.id_comision,pe.codigo_pedido,
    n.nombre_comercial AS negocio,
    co.base_calculo,co.porcentaje_aplicado,
    co.monto_comision,co.estado,co.fecha_liquidacion
FROM dbo.comision co
INNER JOIN dbo.pedido_negocio pn
    ON pn.id_pedido_negocio=co.id_pedido_negocio
INNER JOIN dbo.pedido pe ON pe.id_pedido=pn.id_pedido
INNER JOIN dbo.negocio n ON n.id_negocio=pn.id_negocio
ORDER BY co.fecha_registro DESC;
GO

/* ============================================================
   26. RESUMEN DE VENTAS POR NEGOCIO
   ============================================================ */

SELECT
    n.id_negocio,
    n.nombre_comercial,
    COUNT(pn.id_pedido_negocio) AS cantidad_subpedidos,
    COALESCE(SUM(CASE WHEN pn.estado<>'CANCELADO'
                      THEN pn.subtotal_productos ELSE 0 END),0)
        AS ventas_productos,
    COALESCE(SUM(CASE WHEN pn.estado<>'CANCELADO'
                      THEN pn.costo_delivery ELSE 0 END),0)
        AS total_delivery
FROM dbo.negocio n
LEFT JOIN dbo.pedido_negocio pn ON pn.id_negocio=n.id_negocio
GROUP BY n.id_negocio,n.nombre_comercial
ORDER BY ventas_productos DESC;
GO

/* ============================================================
   27. PRODUCTOS POR CATEGORIA
   COALESCE DENTRO DE SUM EVITA LA ADVERTENCIA POR NULL.
   ============================================================ */

SELECT
    cat.id_categoria,
    cat.nombre AS categoria,
    COUNT(p.id_producto) AS cantidad_productos,
    SUM(COALESCE(p.stock,0)) AS unidades_en_stock
FROM dbo.categoria cat
LEFT JOIN dbo.producto p ON p.id_categoria=cat.id_categoria
GROUP BY cat.id_categoria,cat.nombre
ORDER BY cantidad_productos DESC;
GO

/* ============================================================
   28. PEDIDOS PENDIENTES DE PAGO
   ============================================================ */

SELECT
    pe.codigo_pedido,c.correo,pe.total,
    pe.estado_pedido,pe.estado_pago
FROM dbo.pedido pe
INNER JOIN dbo.cuenta c ON c.id_cuenta=pe.id_cuenta
WHERE pe.estado_pago='PENDIENTE'
ORDER BY pe.fecha_pedido;
GO

/* ============================================================
   29. TOTAL PAGADO POR PEDIDO
   ============================================================ */

SELECT
    pe.codigo_pedido,
    pe.total AS total_pedido,
    COALESCE(SUM(
        CASE WHEN pa.estado_pago='CONFIRMADO'
             THEN pa.monto ELSE 0 END
    ),0) AS total_pagado
FROM dbo.pedido pe
LEFT JOIN dbo.pago pa ON pa.id_pedido=pe.id_pedido
GROUP BY pe.codigo_pedido,pe.total
ORDER BY pe.codigo_pedido;
GO

/* ============================================================
   30. NEGOCIOS Y PRODUCTOS DISPONIBLES
   ============================================================ */

SELECT
    n.nombre_comercial,
    COUNT(p.id_producto) AS productos_publicados,
    SUM(COALESCE(p.stock,0)) AS unidades_disponibles
FROM dbo.negocio n
LEFT JOIN dbo.producto p
    ON p.id_negocio=n.id_negocio
   AND p.estado_publicacion='PUBLICADO'
   AND p.activo=1
   AND p.stock>0
   AND p.fecha_limite_venta>=DATEADD(MINUTE,1,SYSDATETIME())
GROUP BY n.nombre_comercial
ORDER BY productos_publicados DESC;
GO
