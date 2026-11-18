# YAPAA — Modelado del negocio y del sistema

Versión de referencia: implementación local verificada el 17 de noviembre de 2026. Este contenido sustituye los ejemplos de boletos y rutas de los apartados VI.2 y VI.3 del informe. Los diagramas `.drawio` son editables en Draw.io; los `.svg` pueden insertarse en el informe sin perder nitidez.

## 1. Alcance y reglas de negocio comunes

YAPAA conecta establecimientos gastronómicos con clientes interesados en excedentes alimentarios. Permite publicar ofertas, reservar existencias mediante pedidos y continuar la coordinación por WhatsApp. El negocio confirma en YAPAA la venta que efectivamente se realizó. Esa confirmación genera una comisión pendiente de liquidación; no genera un pago del cliente.

| ID | Regla implementada |
|---|---|
| RN-01 | Las cuentas CLIENTE y NEGOCIO son independientes. Un cliente utiliza otro correo para registrar su negocio; conserva su cuenta de compras. |
| RN-02 | El catálogo es público. Para registrar un pedido se necesita una cuenta CLIENTE activa. NEGOCIO no puede comprar ni acceder al catálogo público. |
| RN-03 | La afiliación consulta el RUC en el servidor, exige estado ACTIVO y condición HABIDO y utiliza la razón social devuelta. El nombre comercial puede ser distinto. |
| RN-04 | La nueva afiliación queda PENDIENTE. Solo ADMIN aprueba, rechaza o suspende negocios. |
| RN-05 | Un negocio administra únicamente sus datos, productos y pedidos; ADMIN tiene permisos de supervisión. |
| RN-06 | El catálogo y la compra requieren producto activo, PUBLICADO, con existencias y plazo vigente, perteneciente a un negocio activo y APROBADO. Al crear un pedido se exige un minuto de margen. |
| RN-07 | El servidor toma los precios de los productos; registra pedido, subpedidos y detalles y descuenta existencias transaccionalmente. Un pedido puede contener productos de varios negocios. |
| RN-08 | Cada nuevo negocio tiene un vínculo NegocioPlan. Promocional aplica 10% durante su primer mes; luego pasa a Normal, con 15%. Pro aplica 20% e incorpora indicadores y exportación CSV. |
| RN-09 | El negocio puede elegir Normal o Pro. El cambio es inmediato para futuras confirmaciones. No puede solicitar nuevamente la promoción. El historial de vínculos se conserva. |
| RN-10 | Solo confirmar una venta realizada genera comisión. Abrir WhatsApp, registrar el pedido o cambiar un estado intermedio no genera comisión. |
| RN-11 | Base de comisión = subtotal de productos del subpedido, excluyendo delivery. Monto = base × tasa / 100, con dos decimales y redondeo HALF_UP. La tasa corresponde al plan vigente al confirmar la venta. |
| RN-12 | Existe una sola Comisión por PedidoNegocio. Confirmar repetidamente no crea comisiones adicionales. Cambiar el plan no modifica comisiones ya generadas. |
| RN-13 | Cancelar un pedido no concretado devuelve sus existencias una sola vez y no genera comisión. No se permite cancelar una venta COMPLETADA mediante este flujo. |
| RN-14 | La comisión nace PENDIENTE. ADMIN registra LIQUIDADA tras comprobar externamente el abono del negocio. La aplicación no ejecuta transferencias ni cobra automáticamente. |
| RN-15 | Los indicadores y la exportación CSV exigen Pro en el servidor, además de verificar la propiedad del negocio. Normal y Promocional conservan la consulta de sus pedidos y sus comisiones. |
| RN-16 | Los vencimientos se muestran con cuenta regresiva. Solo los 22 productos identificados de demostración se renuevan automáticamente al vencer; los productos reales no se reinician. |

La política comercial elegida es aplicar la tasa al confirmar la venta, no congelarla al solicitar el pedido. Se informa expresamente en la pantalla de cambio de plan.

## 2. VI.2. Modelado del negocio

### 2.1. Actores y trabajador del negocio

| Participante | Tipo | Interés o responsabilidad |
|---|---|---|
| Consumidor | Actor del negocio | Solicitar y recibir alimentos ofertados a precios reducidos. |
| Establecimiento gastronómico | Actor del negocio | Afiliarse, ofrecer excedentes, concretar ventas y cumplir las comisiones pactadas. |
| Administrador de YAPAA | Trabajador interno | Revisar afiliaciones, supervisar la operación y registrar liquidaciones verificadas. |

La API de RUC y WhatsApp son apoyos tecnológicos, no actores del negocio con objetivos comerciales propios. En los modelos del sistema se precisa su intervención.

### 2.2. Diagrama general de casos de uso del negocio

Archivo: `01-casos-uso-negocio.drawio`. Vista para insertar: `01-casos-uso-negocio.svg`.

### 2.3. Especificaciones de los casos de uso del negocio

#### CUN-01. Afiliar establecimiento

- **Actor principal:** establecimiento gastronómico. **Trabajador:** administrador de YAPAA.
- **Objetivo:** habilitar la participación comercial de un establecimiento identificado.
- **Precondición:** disponer de datos del titular, RUC consultable y un correo que no corresponda a una cuenta existente.
- **Flujo principal:** (1) El establecimiento solicita su afiliación. (2) Proporciona los datos de una nueva cuenta y del local. (3) Se consulta su RUC y se obtiene la razón social. (4) Se registra la cuenta independiente y su afiliación pendiente, con plan inicial. (5) El administrador revisa los datos. (6) Aprueba la afiliación. (7) El establecimiento puede ofrecer productos públicamente.
- **Alternativas:** RUC no activo/habido, duplicado, correo utilizado o datos incompletos impiden el registro; una falla del proveedor permite reintentar. El administrador puede rechazar o suspender la afiliación. Un negocio pendiente puede preparar productos, pero no aparece públicamente.
- **Resultado:** establecimiento registrado, cuenta separada y decisión de aprobación identificable.
- **Reglas:** RN-01, RN-03, RN-04, RN-08.

#### CUN-02. Ofrecer excedentes alimentarios

- **Actor principal:** establecimiento gastronómico.
- **Objetivo:** poner a disposición excedentes aptos para el consumo durante un plazo limitado.
- **Precondición:** cuenta de negocio y datos del producto disponibles; para exposición pública, afiliación aprobada y activa.
- **Flujo principal:** (1) El establecimiento identifica su excedente. (2) Registra nombre, categoría, descripción, precios, existencias y vigencia. (3) Publica la oferta. (4) El consumidor puede visualizarla mientras cumpla las condiciones de disponibilidad. (5) El negocio actualiza o retira el producto cuando corresponde.
- **Alternativas:** puede guardar un borrador; precios o datos inválidos se rechazan; producto vencido, retirado o sin existencias deja de ser comprable. El control tecnológico del plazo no certifica por sí mismo la inocuidad física del alimento.
- **Resultado:** oferta disponible o conservada como borrador/retirada según decisión del negocio.
- **Reglas:** RN-05, RN-06, RN-16.

#### CUN-03. Gestionar solicitud de compra

- **Actores:** consumidor y establecimiento gastronómico.
- **Objetivo:** registrar la intención de compra y facilitar su coordinación con el local.
- **Precondición:** productos disponibles y cuenta de cliente activa para registrar el pedido.
- **Flujo principal:** (1) El consumidor consulta ofertas. (2) Selecciona productos y cantidades. (3) Inicia sesión cuando es necesario. (4) Indica recojo o delivery y dirección cuando corresponde. (5) Registra el pedido. (6) Se reservan existencias y se separa el pedido por establecimiento. (7) El consumidor abre el enlace de WhatsApp y decide enviar el mensaje para coordinar.
- **Alternativas:** sin sesión, se solicita identificación; sin existencias o plazo suficiente, se rechaza la operación; si no envía el mensaje, el pedido puede permanecer pendiente. La aplicación no afirma que WhatsApp haya entregado el mensaje.
- **Resultado:** pedido pendiente y enlaces de coordinación disponibles. Todavía no existe una venta confirmada ni una comisión nueva.
- **Reglas:** RN-02, RN-06, RN-07, RN-10.

#### CUN-04. Concretar venta y entrega

- **Actores:** establecimiento gastronómico y consumidor.
- **Objetivo:** completar la comercialización y dejar constancia de la venta realizada.
- **Precondición:** pedido del establecimiento aún no cancelado ni completado.
- **Flujo principal:** (1) Las partes coordinan por WhatsApp. (2) Acuerdan el pago y la entrega fuera de la plataforma. (3) El negocio verifica que la venta se concretó. (4) Confirma la venta realizada en YAPAA. (5) Su subpedido pasa a COMPLETADO. (6) Se determina y registra la comisión según el plan vigente.
- **Alternativas:** si la venta no se concreta, el negocio cancela el pedido; se devuelve el stock una sola vez. Una confirmación repetida devuelve el resultado existente. Una venta completada no se cancela mediante este flujo.
- **Resultado:** venta confirmada y comisión pendiente, o pedido cancelado sin comisión.
- **Reglas:** RN-10, RN-11, RN-12, RN-13.

#### CUN-05. Gestionar plan y comisión

- **Actor principal:** establecimiento gastronómico. **Trabajador:** administrador de YAPAA.
- **Objetivo:** aplicar las condiciones comerciales del servicio y registrar la liquidación de sus comisiones.
- **Precondición:** afiliación registrada y vínculo comercial.
- **Flujo principal:** (1) El negocio consulta su plan y porcentaje. (2) Puede elegir Normal o Pro tras conocer las condiciones. (3) Consulta las comisiones de sus ventas realizadas. (4) Abona las comisiones a YAPAA por el medio acordado fuera de la plataforma. (5) El administrador verifica el abono. (6) Registra su liquidación.
- **Alternativas:** Pro permite indicadores y exportación; Normal no permite esas funciones. La promoción vence y da paso a Normal. Las comisiones ya generadas conservan su porcentaje.
- **Resultado:** plan vigente identificado y comisiones consultables como pendientes o liquidadas.
- **Reglas:** RN-08, RN-09, RN-11, RN-12, RN-14, RN-15.

### 2.4. Objetivos del negocio

Estos objetivos describen resultados perseguidos; no constituyen impactos ya medidos.

| ID | Objetivo | Indicador propuesto | Caso relacionado |
|---|---|---|---|
| ON-01 | Facilitar la comercialización oportuna de excedentes. | Ventas confirmadas y unidades vendidas antes del plazo. | CUN-02, CUN-03, CUN-04 |
| ON-02 | Facilitar la afiliación de establecimientos identificados. | Solicitudes registradas, aprobadas y rechazadas. | CUN-01 |
| ON-03 | Ofrecer acceso a alimentos a precios reducidos. | Productos ofertados con precio inferior al normal. | CUN-02, CUN-03 |
| ON-04 | Generar ingresos para YAPAA mediante comisiones transparentes. | Comisiones pendientes y liquidadas por venta. | CUN-04, CUN-05 |
| ON-05 | Apoyar decisiones del establecimiento a partir de sus ventas. | Ventas, unidades, subtotal y neto tras comisión disponibles en Pro. | CUN-05 |

## 3. VI.3. Modelado del sistema

### 3.1. Actores del sistema

| Actor | Descripción y permisos |
|---|---|
| Visitante | Persona sin sesión: consulta catálogo, registra cliente, solicita cuenta de negocio e inicia sesión. |
| Cliente | Cuenta CLIENTE: registra pedidos y consulta los propios. Puede solicitar otra cuenta de negocio sin convertir su cuenta actual. |
| Negocio | Cuenta NEGOCIO: administra sus negocios, productos, plan y pedidos. Confirma ventas, cancela solicitudes no concretadas y consulta comisiones propias. |
| Administrador | Cuenta ADMIN: revisa afiliaciones y registra liquidaciones; dispone de supervisión autorizada por la API. |
| Servicio de consulta RUC | Decolecta: responde consultas de identidad fiscal realizadas exclusivamente por Spring Boot. |

WhatsApp es un sistema externo de continuación manual. La web genera un enlace `wa.me`; no existe una integración que informe automáticamente recepción del mensaje, pago, entrega o confirmación de compra. Por esa razón no se lo representa como actor que confirme ventas a YAPAA.

### 3.2. Requerimientos funcionales y trazabilidad

| ID | Requerimiento | Prioridad | Objetivo | Caso de uso |
|---|---|---|---|---|
| RF-01 | Registrar cuentas de cliente. | Alta | ON-03 | CUS-01 |
| RF-02 | Autenticar cuentas y aplicar permisos por rol y propiedad. | Alta | ON-02 | CUS-02; transversal |
| RF-03 | Consultar catálogo, categorías y detalle de ofertas vigentes. | Alta | ON-01, ON-03 | CUS-03 |
| RF-04 | Registrar cuentas de negocio independientes y afiliación pendiente. | Alta | ON-02 | CUS-04 |
| RF-05 | Consultar RUC y autocompletar razón social. | Alta | ON-02 | CUS-05 |
| RF-06 | Consultar y actualizar datos del propio negocio. | Alta | ON-02 | CUS-06 |
| RF-07 | Crear, editar, retirar y reactivar productos propios. | Alta | ON-01 | CUS-07 |
| RF-08 | Registrar un pedido con grupos por negocio y modalidad de entrega. | Alta | ON-01, ON-03 | CUS-08 |
| RF-09 | Verificar disponibilidad y reservar stock de forma transaccional. | Alta | ON-01 | CUS-09 |
| RF-10 | Generar enlace de WhatsApp con resumen del subpedido. | Alta | ON-01 | CUS-10 |
| RF-11 | Consultar pedidos de la cuenta cliente autenticada. | Alta | ON-03 | CUS-11 |
| RF-12 | Consultar pedidos y comisiones del negocio propietario. | Alta | ON-04 | CUS-12 |
| RF-13 | Confirmar manualmente una venta realizada. | Alta | ON-01, ON-04 | CUS-13 |
| RF-14 | Generar comisión única por subpedido según el plan vigente. | Alta | ON-04 | CUS-14 |
| RF-15 | Cancelar solicitudes no concretadas y devolver existencias. | Alta | ON-01 | CUS-15 |
| RF-16 | Asignar y consultar el plan vigente del establecimiento. | Alta | ON-04 | CUS-16 |
| RF-17 | Cambiar entre Normal y Pro conservando el historial. | Alta | ON-04, ON-05 | CUS-17 |
| RF-18 | Consultar indicadores y exportar CSV con Plan Pro. | Media | ON-05 | CUS-18 |
| RF-19 | Revisar, aprobar, rechazar o suspender afiliaciones. | Alta | ON-02 | CUS-19 |
| RF-20 | Consultar comisiones y registrar liquidaciones verificadas. | Alta | ON-04 | CUS-20 |

### 3.3. Requerimientos no funcionales y evidencia

| ID | Requerimiento | Implementación o verificación |
|---|---|---|
| RNF-01 | Controlar el acceso por rol y propiedad de los recursos. | JWT, `SeguridadConfig`, `UsuarioActual`; pruebas de acceso ajeno y restricciones de plan. |
| RNF-02 | Mantener consistencia entre pedido, stock y comisión. | Transacciones, bloqueo de filas y restricción única Comisión–PedidoNegocio; pruebas de confirmación y cancelación repetidas. |
| RNF-03 | Permitir uso en pantallas móviles y de escritorio. | Verificación de interfaz comercial en 320, 390, 768 y 1440 px sin desbordamiento. No equivale a una certificación de accesibilidad. |
| RNF-04 | Mantener separación entre presentación, negocio y persistencia. | Angular y API separadas; controladores, servicios, repositorios y entidades. |
| RNF-05 | Mantener la credencial del proveedor RUC fuera del navegador. | Consulta en Spring Boot y archivo local excluido de Git. |
| RNF-06 | Actualizar el tiempo restante de las ofertas. | Reloj de interfaz cada segundo; catálogo consulta periódicamente la disponibilidad. |

No se afirma una disponibilidad de producción, un tiempo de respuesta garantizado, cumplimiento legal integral ni resultados de pruebas humanas sin evidencias específicas.

### 3.4. Diagramas de casos de uso del sistema

- `02-casos-uso-sistema-general.drawio`: vista general por objetivos de los actores.
- `03-sistema-compra.drawio`: detalle de CUS-08 con inclusión de CUS-09 y CUS-10.
- `04-sistema-afiliacion-comisiones.drawio`: detalle de CUS-04/CUS-05 y CUS-13/CUS-14.

Las flechas «include» apuntan al caso incluido y representan comportamiento obligatorio. La autenticación figura como precondición de los casos protegidos; no se dibuja como inclusión repetida. Pro es una precondición de CUS-18, no un actor ni una relación «extend».

### 3.5. Especificaciones de casos de uso del sistema

#### CUS-01. Registrar cliente

**Actor:** visitante. **Precondición:** correo no registrado. **Flujo:** ingresar datos personales y contraseña; validar campos y correo; cifrar contraseña con BCrypt; persistir Cuenta y Cliente con rol CLIENTE; devolver sesión. **Alternativas:** correo duplicado, rol no permitido o datos incompletos se rechazan. **Resultado:** cuenta activa CLIENTE. **Código:** `AuthService.registrar`, `POST /api/auth/registro`.

#### CUS-02. Iniciar sesión

**Actor:** visitante que posee cuenta. **Flujo:** ingresar credenciales; autenticar; obtener roles; emitir JWT; redirigir a catálogo, panel de negocio o administración según rol. **Alternativas:** credenciales inválidas o cuenta inactiva impiden el acceso. **Resultado:** sesión autenticada. **Código:** `AuthService.login`, `POST /api/auth/login`, `Sesion`, `rol-guard`. Los alias cliente/negocio/admin solo están habilitados en demostración.

#### CUS-03. Consultar catálogo y detalle

**Actores:** visitante y cliente. **Flujo:** consultar ofertas disponibles; filtrar por categoría; revisar fotografía, precios, negocio y cuenta regresiva; abrir detalle. **Alternativas:** sin resultados, se informa la ausencia de ofertas; vencidos o agotados no se compran; una cuenta NEGOCIO recibe rechazo en la API y redirección en Angular. **Resultado:** información de ofertas consultada. **Código:** `ProductoController`, `ProductoRepository.buscarDisponibles`, `buscarDisponiblesPorCategoria`, componentes de catálogo.

#### CUS-04. Solicitar afiliación de negocio

**Actor:** visitante o cliente que solicita otra cuenta. **Precondición:** correo distinto y no registrado. **Incluye:** CUS-05. **Flujo:** ingresar nuevas credenciales, RUC y datos comerciales; consultar RUC; mostrar razón social de solo lectura; validar ACTIVO/HABIDO; crear Cuenta NEGOCIO y Negocio PENDIENTE; asignar plan inicial; informar que requiere aprobación. **Alternativas:** duplicidad, RUC inválido, proveedor no disponible o campos incompletos impiden finalizar. **Resultado:** negocio pendiente con cuenta independiente; la sesión cliente existente no se convierte. **Código:** `AuthService.registrarNegocio`, `NegocioService.registrarNuevaCuenta`, `PlanService.vigente`, `POST /api/auth/registro-negocio`, `Afiliacion`.

#### CUS-05. Consultar RUC

**Actor de apoyo:** servicio externo de consulta RUC. **Flujo:** recibir un RUC con formato válido; consultar desde el servidor o usar caché vigente; devolver razón social, estado y condición; mostrar el resultado en Angular. **Alternativas:** errores del proveedor se informan; editar el RUC limpia el resultado anterior. En demostración, dos RUC ficticios se identifican con `demostracion=true` y `verificado=false`. **Resultado:** identidad fiscal disponible para la afiliación. **Código:** `ConsultaRucService`, `GET /api/negocios/ruc/{ruc}`. El servicio externo no valida que el nombre comercial sea idéntico a la razón social.

#### CUS-06. Administrar cuenta de negocio

**Actor:** negocio propietario. **Precondición:** sesión NEGOCIO y propiedad del negocio. **Flujo:** consultar datos propios; editar nombre comercial, dirección, distrito, ciudad y teléfono; validar; guardar. **Alternativas:** datos incompletos se rechazan; la razón social no se sustituye por un texto enviado por el navegador. Si se cambia RUC mediante la API, se vuelve a consultar. **Resultado:** datos actualizados sin cambiar la propiedad. **Código:** `NegocioService.actualizar`, `GET /api/negocios/mios`, `PUT /api/negocios/{id}`, `PanelNegocio`.

#### CUS-07. Gestionar productos

**Actor:** negocio propietario. **Flujo:** listar productos propios; crear o editar datos, categoría, fotografía opcional, precios, stock, vigencia y estado de publicación; guardar; retirar o reactivar cuando corresponda. **Alternativas:** precios inválidos, negocio ajeno o campos obligatorios faltantes se rechazan; un producto puede permanecer como borrador. **Resultado:** producto propio actualizado, sujeto a condiciones de exposición pública. **Código:** `ProductoService`, `/api/productos/mios`, POST/PUT de productos y PATCH `/{id}/activo`, `PanelNegocio`.

#### CUS-08. Registrar pedido

**Actor:** cliente. **Precondición:** sesión CLIENTE activa y carrito no vacío. **Incluye:** CUS-09 y CUS-10. **Flujo:** agrupar productos por negocio; elegir RECOJO o DELIVERY; indicar dirección si aplica; enviar solicitud; verificar cada producto; tomar precio del servidor; descontar stock; persistir Pedido, PedidoNegocio y DetallePedido; devolver resumen y enlace de WhatsApp por negocio. **Alternativas:** si falla una validación o no alcanza el stock, la transacción no finaliza; no se permite comprar desde NEGOCIO. **Resultado:** pedido PENDIENTE, sin nuevo Pago ni Comisión. **Código:** `PedidoService.crear`, `POST /api/pedidos`, carrito Angular.

#### CUS-09. Validar disponibilidad y reservar stock

**Actor:** ejecutado como parte de CUS-08. **Flujo:** verificar pertenencia de cada producto al negocio; comprobar activo, PUBLICADO, stock y plazo; comprobar afiliación aprobada y activa; descontar la cantidad solicitada en la transacción. **Alternativas:** producto repetido en un grupo o datos no disponibles producen error. **Resultado:** stock reservado o operación revertida. **Código:** `PedidoService.construirSubpedido`, `validarDisponible`, `ProductoRepository.buscarParaActualizar` y `descontarStock`.

#### CUS-10. Generar enlace WhatsApp

**Actor:** ejecutado como parte de CUS-08; el cliente utiliza el resultado. **Flujo:** construir mensaje con código, productos, cantidades, importes y modalidad; normalizar teléfono; codificar mensaje; devolver enlace `https://wa.me/...`. **Alternativas:** si el negocio no tiene teléfono utilizable, no se obtiene enlace válido. **Resultado:** enlace disponible; no se envía automáticamente ningún mensaje. **Código:** `PedidoService.construirMensaje`, `construirEnlaceWhatsApp`.

#### CUS-11. Consultar mis pedidos

**Actor:** cliente. **Precondición:** cuenta propietaria del pedido. **Flujo:** consultar listado paginado; abrir detalle; revisar estado, grupos, productos y enlaces. **Alternativas:** cuenta o pedido ajeno devuelve acceso denegado. **Resultado:** información propia consultada. **Código:** `PedidoService.listarPorCuenta`/`obtenerPorId`, GET `/api/pedidos/cuenta/{idCuenta}` y `/{id}`, componentes de pedidos.

#### CUS-12. Consultar pedidos y comisiones del negocio

**Actor:** negocio propietario. **Flujo:** consultar lista paginada de subpedidos; revisar productos, modalidad, subtotal, delivery, estado de venta e importe y estado de comisión. **Alternativas:** sin pedidos, se muestra estado vacío; recursos ajenos no son accesibles. **Resultado:** datos comerciales propios consultados. **Código:** `VentaService.listar`, GET `/api/negocios/{id}/ventas`, `GestionComercial`.

#### CUS-13. Confirmar venta realizada

**Actor:** negocio propietario. **Precondición:** negocio activo y aprobado; subpedido no cancelado. **Incluye:** CUS-14. **Flujo:** seleccionar pedido; leer la confirmación de que la venta ya se concretó; confirmar; bloquear el pedido y subpedido; obtener plan vigente; generar comisión si no existe; marcar subpedido COMPLETADO; actualizar el estado global cuando todos los subpedidos son terminales; devolver resultado. **Alternativas:** cancelado se rechaza; completado devuelve resultado existente sin duplicar comisión; ajeno se rechaza. **Resultado:** venta completada y comisión registrada. **Código:** `VentaService.confirmar`, POST `/api/ventas/{id}/confirmar`.

#### CUS-14. Generar comisión

**Actor:** ejecutado como parte de CUS-13. **Flujo:** obtener tasa del NegocioPlan vigente; tomar subtotal de productos; calcular monto con dos decimales; crear Comisión PENDIENTE asociada al PedidoNegocio. **Alternativas:** si ya existe una comisión, no duplicarla; si no existe plan utilizable, no finalizar la confirmación. **Resultado:** una comisión por venta, con tasa e importe históricos. **Código:** `VentaService.confirmar`, entidad `Comision`, restricción única `id_pedido_negocio`. Ejemplo: productos S/19.80 y delivery S/7.00 con Normal → comisión S/2.97.

#### CUS-15. Cancelar pedido no concretado

**Actor:** negocio propietario. **Precondición:** subpedido no completado y sin comisión generada. **Flujo:** solicitar cancelación; confirmar; bloquear registros; devolver cantidades a los productos; marcar CANCELADO; actualizar estado global si corresponde. **Alternativas:** repetir devuelve el resultado sin sumar stock otra vez; venta realizada no se cancela por esta vía. **Resultado:** pedido cancelado sin comisión. **Código:** `VentaService.cancelar`, POST `/api/ventas/{id}/cancelar`.

#### CUS-16. Asignar y consultar plan vigente

**Actor:** negocio propietario; asignación automática como parte de la afiliación. **Flujo:** consultar vínculo activo; asignar inicial si falta; si la promoción o vínculo venció, finalizarlo y asignar Normal; devolver nombre, tasa, fechas y capacidad de reportes. **Alternativas:** plan con fecha inicial futura o catálogo de planes no disponible produce error. **Resultado:** vínculo vigente identificado. **Código:** entidades `PlanComercial` y `NegocioPlan`, `PlanService.vigente`/`consultar`, GET `/api/negocios/{id}/plan`. La transición vencida se resuelve al consultar o usar el plan, sin depender de refrescar la interfaz.

#### CUS-17. Cambiar plan

**Actor:** negocio propietario. **Precondición:** sesión autorizada. **Flujo:** consultar Normal/Pro; revisar tasas y capacidades; elegir y confirmar; finalizar vínculo anterior; crear nuevo vínculo; actualizar panel. **Alternativas:** mismo plan no genera otro vínculo; pedir Promocional se rechaza; negocio ajeno se rechaza. **Resultado:** plan nuevo para próximas ventas confirmadas, sin modificar comisiones anteriores. **Código:** `PlanService.cambiar`, PUT `/api/negocios/{id}/plan`, `GestionComercial`.

#### CUS-18. Consultar indicadores y exportar reporte

**Actor:** negocio propietario. **Precondición:** Pro vigente. **Flujo:** validar propiedad y plan; obtener ventas COMPLETADAS; sumar ventas, unidades, subtotal, comisión no anulada y neto; mostrar indicadores; permitir descargar CSV con ventas y comisiones. **Alternativas:** Normal/Promocional o negocio ajeno recibe 403; sin ventas se muestran ceros. **Resultado:** indicadores y archivo CSV propios. **Código:** `PlanService.exigirPro`, `VentaService.resumen`/`csv`, GET `/api/negocios/{id}/indicadores` y `/reporte.csv`. El reporte no afirma que se hayan recibido pagos; el neto es subtotal menos comisión.

#### CUS-19. Revisar afiliaciones

**Actor:** administrador. **Flujo:** filtrar afiliaciones; revisar información; aprobar, rechazar o suspender según corresponda; persistir estado. **Alternativas:** otro rol no está autorizado. **Resultado:** decisión de publicación actualizada. **Código:** `NegocioService.cambiarEstadoAprobacion`, PATCH `/api/negocios/{id}/aprobacion`, `Administracion`.

#### CUS-20. Consultar y liquidar comisiones

**Actor:** administrador. **Precondición para liquidar:** comisión PENDIENTE y abono verificado fuera del sistema. **Flujo:** consultar comisiones; revisar negocio, base, porcentaje e importe; verificar el abono; confirmar registro de liquidación; marcar LIQUIDADA y registrar fecha. **Alternativas:** estado distinto de pendiente impide liquidar; sin verificación, conservar pendiente. La API administrativa también contempla anular comisiones no liquidadas como corrección; no ejecuta devoluciones bancarias. **Resultado:** liquidación administrativa registrada. **Código:** `ComisionService.liquidar`, GET `/api/comisiones`, PATCH `/api/comisiones/{id}/liquidar`, `ComisionesAdmin`.

## 4. Coherencia del modelo con el código

### 4.1. Relaciones del dominio relevantes

| Relación | Interpretación |
|---|---|
| Cuenta → Cliente / Negocio | Roles y perfiles; la afiliación nueva crea otra Cuenta, no convierte la Cuenta cliente. |
| Negocio → NegocioPlan → PlanComercial | Asociación comercial con fechas, estado y porcentaje aplicado; conserva historial. |
| Negocio → Producto → Categoria | Oferta propia del establecimiento clasificada por categoría. |
| Cuenta → Pedido → PedidoNegocio → DetallePedido | Pedido del cliente separado por local y sus productos. |
| PedidoNegocio → Comisión | Una comisión como máximo por venta de un negocio. |

No se crea una clase duplicada `Planes`: `PlanComercial` ya representa ese concepto y `NegocioPlan` representa la vinculación solicitada.

### 4.2. Estados y límites

- Afiliación: PENDIENTE, APROBADO, RECHAZADO, SUSPENDIDO.
- Vínculo de plan: ACTIVO, FINALIZADO, CANCELADO. El cambio de plan conserva el anterior como FINALIZADO.
- Flujo de pedidos de la interfaz comercial: PENDIENTE → COMPLETADO o CANCELADO por subpedido.
- La entidad conserva estados intermedios existentes para gestión administrativa; el cambio genérico de estado no permite completar/cancelar ventas, ni reabrir pedidos que ya contienen ventas terminales.
- El pedido global pasa a CANCELADO si todos sus subpedidos se cancelan; pasa a COMPLETADO si todos son terminales y al menos uno se completó. Mientras existan subpedidos abiertos conserva su estado anterior.
- Comisión: PENDIENTE → LIQUIDADA; la corrección administrativa permite ANULADA antes de liquidar.
- Las tablas históricas de pagos se conservan, pero las rutas de pago están bloqueadas y el flujo nuevo no inserta Pago.

### 4.3. Evidencia técnica de la versión

Se ejecutaron 22 pruebas de API sin fallos, incluidas pruebas transaccionales con SQL Server y reversión de datos. Verifican separación de cuentas, permisos, pedido sin pago/comisión inicial, tasas 10/15/20, exclusión de delivery, conservación de tasa histórica, confirmación repetida, cancelación repetida, vencimiento de promoción y protección de reportes Pro.

Angular compiló correctamente. Se comprobó en navegador el plan real del negocio, las restricciones en la API, confirmación previa al cambio de plan y pantalla de liquidaciones del administrador. La vista Pro se comprobó con respuestas controladas de interfaz; sus operaciones reales se verificaron en las pruebas transaccionales. Estas evidencias no sustituyen pruebas de aceptación o usabilidad con participantes.

### 4.4. Aspectos del informe que deben expresarse con precisión

1. La comisión permanece como modelo de ingresos aunque la coordinación ocurra en WhatsApp.
2. YAPAA registra una obligación de comisión; cobrar y verificar el abono es una gestión externa. No hay pasarela ni débito automático.
3. Pro incluye indicadores concretos de ventas, unidades, subtotal, comisiones y neto, más CSV. No se deben atribuir análisis predictivos o estadísticas de desperdicio físico que el código no calcula.
4. El delivery lo coordina el negocio. La aplicación no administra una flota ni comprueba la entrega.
5. No se deben representar notificaciones automáticas como funcionalidad ya implementada sin sus rutas, servicios y pruebas.
6. La simulación de ofertas es solo demostrativa; no demuestra ventas reales, ingresos efectivos o reducción medida de desperdicios.
7. Los diagramas de clases, DER, componentes y despliegue son otros entregables de VI.4 y no se sustituyen con estos casos de uso.

## 5. Archivos de esta entrega

- `01-casos-uso-negocio.drawio` y `.svg`.
- `02-casos-uso-sistema-general.drawio` y `.svg`.
- `03-sistema-compra.drawio` y `.svg`.
- `04-sistema-afiliacion-comisiones.drawio` y `.svg`.
- Este documento: objetivos, reglas, requerimientos, 5 casos del negocio y 20 casos del sistema.

Los archivos de documentación no cambian la estructura de la aplicación. El modelado se basa en el comportamiento implementado; cualquier futura modificación de planes, estados, cobros o permisos requiere actualizar esta versión.
