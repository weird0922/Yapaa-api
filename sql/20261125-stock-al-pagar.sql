-- Ejecutar sobre YAPAA_BD antes de actualizar la API.
-- NULL conserva el comportamiento de los pedidos escritos por la API anterior.
IF COL_LENGTH('dbo.pedido', 'stock_descontado') IS NULL
    ALTER TABLE dbo.pedido ADD stock_descontado BIT NULL;
