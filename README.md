# YAPAA API

API de YAPAA desarrollada con Java 21, Spring Boot, Spring Security/JWT, JPA y SQL Server. La interfaz Angular se mantiene en [Yapaa-web](https://github.com/weird0922/Yapaa-web).

### Arranque de demostración en Windows, sin Docker

Con Java 21, SQL Server en localhost:1433 y sqlcmd instalados, ejecutar
**Iniciar-API.cmd** desde esta carpeta. El asistente usa la contraseña del
usuario sa del SQL Server local y crea o actualiza únicamente la base
**YAPAA_DEMO_GIT**. Para el OTP usa el remitente ya configurado o solicita su
contraseña de aplicación de Gmail la primera vez. El correo remitente
sugerido es **yapaaservice@gmail.com**.

Cuenta administrativa de esta base: **admin@admin.com / YapaaDemo2026!**.
Después, iniciar Yapaa-web y entrar por **http://localhost:4200/login**.


## Ejecución local

1. Instalar JDK 21 y disponer de SQL Server con la base `YAPAA_BD` y su esquema existente.
2. Copiar `config/app-local.properties.example` como `config/app-local.properties` y completar los valores. También pueden configurarse `YAPAA_DB_PASSWORD`, `YAPAA_JWT_SECRET` y `YAPAA_RUC_TOKEN` mediante variables de entorno. La clave JWT debe tener al menos 32 bytes.
3. Desde la carpeta de la API, ejecutar `./mvnw spring-boot:run` o `mvnw.cmd spring-boot:run` en Windows.
4. API local: `http://localhost:8080`; Swagger: `http://localhost:8080/swagger-ui/index.html`.

Los archivos `config/*-local.properties` se excluyen de Git. La integración RUC usa Decolecta desde el servidor; Angular nunca recibe su clave. La configuración local actual se conserva al preparar el repositorio.

Los scripts SQL históricos de creación pueden eliminar y recrear la base de demostración. No ejecutarlos sobre una base con información que deba conservarse. Los cambios de planes de esta versión utilizan las tablas existentes y no requieren recrear la base.

## Comportamiento comercial

- Cuentas CLIENTE, NEGOCIO y ADMIN separadas. Un cliente registra una cuenta negocio con otro correo.
- Afiliación con consulta de RUC y aprobación administrativa.
- Productos con precio reducido, existencias y vigencia; descuento transaccional al aprobar el pago simulado.
- Pedido pendiente y enlace WhatsApp por negocio, sin crear un pago.
- El pago aprobado confirma el pedido y aplica automáticamente una comisión única por subpedido. El negocio marca después la entrega; esta acción no vuelve a cobrar comisión.
- Comisión sobre subtotal de productos, excluyendo delivery. Tasa vigente al aprobar el pago, redondeada a dos decimales.
- Promocional: 10% durante el primer mes de registro. Luego Normal: 15%. Pro: 20%, con indicadores y CSV.
- La cancelación de un pago antes de la entrega cancela el pedido, restituye stock una sola vez y anula sus comisiones. Los pedidos entregados no se cancelan mediante este flujo.
- No existe registro manual de liquidaciones. Los importes se actualizan en la misma transacción del pago; repetir una aprobación no duplica pagos ni comisiones.

Las entidades `PlanComercial`, `NegocioPlan` y `Comision` conservan la estructura existente. Las comisiones anteriores no cambian al elegir otro plan.

## Rutas comerciales principales

| Operación | Ruta | Acceso |
|---|---|---|
| Listar planes elegibles | GET `/api/planes` | Público |
| Consultar/cambiar plan | GET/PUT `/api/negocios/{id}/plan` | Propietario o ADMIN |
| Consultar pedidos propios y sus comisiones | GET `/api/negocios/{id}/ventas` | Propietario o ADMIN |
| Marcar entrega de un pedido pagado | POST `/api/ventas/{id}/confirmar` | Propietario o ADMIN |
| Cancelar solicitud no concretada | POST `/api/ventas/{id}/cancelar` | Propietario o ADMIN |
| Consultar indicadores | GET `/api/negocios/{id}/indicadores` | Propietario o ADMIN; exige Pro |
| Exportar ventas CSV | GET `/api/negocios/{id}/reporte.csv` | Propietario o ADMIN; exige Pro |
| Consultar comisiones | GET `/api/comisiones` | ADMIN |
| Resumen y ventas por negocio | GET `/api/dashboard`, GET `/api/dashboard/negocios/{id}` | ADMIN |
| Crear escenario local | POST `/api/dashboard/demostracion` | ADMIN; demostración habilitada |

Cambiar un estado administrativo genérico no completa/cancela ventas ni reabre subpedidos terminales.

## Pruebas

Con SQL Server local disponible y JDK 21:

```text
mvnw.cmd -Dtest=UsuarioActualTest,AfiliacionServiceTest,AfiliacionIntegracionTest,ConsultaRucServiceTest,SimulacionDemoServiceTest test
```

Se verificaron pruebas funcionales y de integración del flujo comercial. Las pruebas de integración crean registros temporales y revierten sus transacciones. El proveedor RUC se simula en esas pruebas para no consumir consultas externas; la consulta real se verificó adicionalmente desde el navegador.

## Demostración y producción

El perfil local habilita demostración. Alias de demostración: cliente/123 y negocio/1234. El administrador utiliza la cuenta y contraseña configuradas privadamente en el servidor. El perfil `prod` deshabilita demostración; el despliegue debe aportar su configuración privada y revisar los demás requisitos operativos del entorno.

La renovación de fechas afecta únicamente los 22 productos originales y los productos del escenario ficticio identificado por RUC 20999. Los productos reales no se reinician. Los dos RUC locales ficticios se señalan como no verificados. Estas simulaciones no representan ventas ni contribuyentes reales.

## Modelado

[Modelo y especificaciones de casos de uso](docs/modelado/MODELO-CASOS-DE-USO.md): 5 casos del negocio, 20 del sistema, reglas, requerimientos y trazabilidad. Incluye diagramas Draw.io editables y versiones SVG/PNG.

## Actualización de acceso y correo

Los registros de cliente y negocio exigen confirmar la contraseña y verificar su correo con un OTP de seis dígitos. El código vence en un minuto, permite cinco intentos y limita los reenvíos. La autorización de registro vence en quince minutos y se consume una sola vez.

Gmail SMTP se configura en config/correo-local.properties, excluido de Git, usando el ejemplo de esa carpeta. Requiere contraseña de aplicación y verificación en dos pasos de Google. El modo local solo funciona con demostración habilitada y no acredita la recepción de un correo real.

El dashboard exige un administrador autenticado autorizado por la API. La web lo carga únicamente tras el login principal. Ocultar una URL no sustituye el control de acceso del servidor.

La pasarela es un simulador interno: permite aprobación, rechazo y anulación sin datos bancarios ni cargos reales. El pago aprobado aplica la comisión sobre productos y conserva su tasa histórica. Delivery no forma parte de la base. El dashboard separa productos, delivery, comisión YAPAA y neto de los negocios; solo incluye pagos confirmados con comisiones aplicadas. Las rutas de liquidación/anulación manual de comisiones se encuentran bloqueadas.



## Escenario comercial de 30 negocios

La creación del escenario conserva los registros existentes y completa hasta 30 negocios, distribuidos entre Normal (18), Pro (9) y Promocional (3). Utiliza correos .test, RUC ficticios, fotografías locales y ventas con precios y nombres de producto conservados en sus detalles. Los códigos D30 y las referencias SIM-D30 identifican los pedidos generados. Repetir la operación no duplica negocios, clientes ni ventas. Las existencias actuales representan un lote repuesto, no el stock anterior a estas ventas históricas.

Las cuentas generadas tienen contraseñas aleatorias no publicadas. El escenario no envía correos ni usa servicios de pago externos. Los periodos de venta se sitúan dentro del mes de creación; no se reescribe el historial financiero al renovar la disponibilidad de los productos.

Las comisiones por línea que muestra el dashboard se reparten proporcionalmente al subtotal, conservando al céntimo la comisión total del subpedido. Cada venta muestra total cobrado, delivery, comisión y neto. El documento de modelado del 17/11 conserva el flujo anterior; el flujo comercial vigente es el descrito aquí.

## Direcciones y delivery — 23/11/2026

Cada cliente puede guardar hasta diez direcciones con nombre, referencia y una predeterminada. La primera se marca automáticamente como predeterminada. La ubicación aceptada queda bloqueada; para cambiarla se agrega otra dirección. Las direcciones se guardan en SQL Server por cuenta y no se comparten entre clientes. Eliminarlas no modifica los textos históricos de los pedidos.

El selector usa Leaflet, OpenStreetMap y Photon: búsqueda con pausa de 800 ms, sugerencias, marcador arrastrable, geocodificación inversa, ubicación del dispositivo y enlace a Google Maps. No utiliza la API pública de Nominatim. Las selecciones que permite guardar el servidor están vinculadas a la cuenta y vencen a los veinte minutos. El negocio fija su punto de recojo en su propio panel; sin este punto solo se permite recojo. Los puntos iniciales de los negocios identificados del escenario son ficticios.

La cobertura inicial es la zona urbana de Trujillo delimitada en `CoberturaTrujillo`, con un máximo de 6 km por calles entre negocio y destino. OSRM calcula la distancia por la red vial; no se cobra según distancia en línea recta. Tarifa inicial: S/ 4 hasta 2 km, más S/ 1 por km adicional, redondeando hacia arriba a S/ 0,50. Hasta 6 km cuesta S/ 8. El importe de reparto no integra la base de comisión de YAPAA.

La cotización del servidor vence a los diez minutos, pertenece a un cliente, negocio y dirección, y se consume una sola vez en la transacción del pedido. El servidor ignora los importes de delivery enviados por el navegador. Al cambiar de dirección se recalcula; con recojo el costo es cero. Si falla el servicio de rutas no se inventa una distancia ni se permite continuar con delivery gratuito.

Las tablas nuevas se crean sin recrear la base. Script: `yapaa-api/sql/20261123-direcciones-delivery.sql`. Servicios configurables: `yapaa.mapas.photon` y `yapaa.mapas.osrm`. Se usan las instancias públicas para el proyecto académico, con caché y límites; para mayor tráfico se requiere un proveedor o instancia con capacidad acordada. Referencias: https://github.com/komoot/photon, https://project-osrm.org/docs/v5.24.0/api/, https://operations.osmfoundation.org/policies/tiles/.

El dashboard solo se abre desde el login administrativo. Escribir o recargar una ruta administrativa borra la sesión local y redirige al catálogo. Las ventas de cada negocio tienen una página separada, con sus datos, totales del periodo, productos y paginación; el regreso conserva las fechas. La API mantiene la autorización ADMIN independientemente de la navegación.

### Stock al confirmar el pago

En una base existente, aplicar primero `sql/20261125-stock-al-pagar.sql` sobre `YAPAA_BD`. La migración agrega una columna nullable sin modificar pedidos ni inventario. Los pedidos anteriores conservan el descuento ya registrado por la API anterior. Los pedidos nuevos descuentan stock al aprobar el pago simulado, dentro de la misma transacción que registra el pago y las comisiones. La disponibilidad se comprueba tanto al crear el pedido como al aprobar el pago, con un minuto mínimo antes del vencimiento. El script `yapaa-scriptsql.sql` recrea la base y se debe usar solo con datos desechables.
