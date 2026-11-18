USE YAPAA_BD;
GO
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
END WHERE imagen_url LIKE 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?%';
