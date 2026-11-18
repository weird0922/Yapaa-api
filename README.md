# YAPAA API

API de YAPAA desarrollada con Java 21, Spring Boot, Spring Security/JWT, JPA y SQL Server. La interfaz Angular se mantiene en [Yapaa-web](https://github.com/weird0922/Yapaa-web).

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
- Productos con precio reducido, existencias y vigencia; reserva transaccional al registrar pedido.
- Pedido pendiente y enlace WhatsApp por negocio, sin crear un pago.
- El negocio confirma manualmente la venta realizada; entonces se genera una comisión única por subpedido.
- Comisión sobre subtotal de productos, excluyendo delivery. Tasa vigente al confirmar, redondeada a dos decimales.
- Promocional: 10% durante el primer mes de registro. Luego Normal: 15%. Pro: 20%, con indicadores y CSV.
- Cancelación de un pedido no completado: restitución de stock una sola vez y sin comisión.
- ADMIN registra liquidaciones únicamente después de verificar externamente el abono del negocio. No hay cargo automático.

Las entidades `PlanComercial`, `NegocioPlan` y `Comision` conservan la estructura existente. Las comisiones anteriores no cambian al elegir otro plan.

## Rutas comerciales principales

| Operación | Ruta | Acceso |
|---|---|---|
| Listar planes elegibles | GET `/api/planes` | Público |
| Consultar/cambiar plan | GET/PUT `/api/negocios/{id}/plan` | Propietario o ADMIN |
| Consultar pedidos propios y sus comisiones | GET `/api/negocios/{id}/ventas` | Propietario o ADMIN |
| Confirmar venta realizada | POST `/api/ventas/{id}/confirmar` | Propietario o ADMIN |
| Cancelar solicitud no concretada | POST `/api/ventas/{id}/cancelar` | Propietario o ADMIN |
| Consultar indicadores | GET `/api/negocios/{id}/indicadores` | Propietario o ADMIN; exige Pro |
| Exportar ventas CSV | GET `/api/negocios/{id}/reporte.csv` | Propietario o ADMIN; exige Pro |
| Consultar/liquidar comisiones | GET `/api/comisiones`, PATCH `/api/comisiones/{id}/liquidar` | ADMIN |

Cambiar un estado administrativo genérico no completa/cancela ventas ni reabre subpedidos terminales.

## Pruebas

Con SQL Server local disponible y JDK 21:

```text
mvnw.cmd -Dtest=UsuarioActualTest,AfiliacionServiceTest,AfiliacionIntegracionTest,ConsultaRucServiceTest,SimulacionDemoServiceTest test
```

Se verificaron 22 pruebas sin fallos. Las pruebas de integración crean registros temporales y revierten sus transacciones. El proveedor RUC se simula en esas pruebas para no consumir consultas externas; la consulta real se verificó adicionalmente desde el navegador.

## Demostración y producción

El perfil local habilita demostración. Alias de acceso: cliente/123, negocio/1234 y admin/admin123. El perfil `prod` deshabilita demostración; el despliegue debe aportar su configuración privada y revisar los demás requisitos operativos del entorno.

La renovación de fechas solo afecta los 22 productos identificados de demostración. Los dos RUC locales ficticios se señalan como no verificados. Estas simulaciones no representan ventas ni contribuyentes reales.

## Modelado

[Modelo y especificaciones de casos de uso](docs/modelado/MODELO-CASOS-DE-USO.md): 5 casos del negocio, 20 del sistema, reglas, requerimientos y trazabilidad. Incluye diagramas Draw.io editables y versiones SVG/PNG.
