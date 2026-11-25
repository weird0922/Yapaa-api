package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Pageable;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.security.UsuarioActual;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import java.time.*;
import java.math.*;
import java.util.*;
@Service
public class EscenarioDemoService {
 private final CuentaRepository cuentas;private final RolRepository roles;private final NegocioRepository negocios;private final NegocioPlanRepository vinculos;private final PlanComercialRepository catalogo;private final ProductoRepository productos;private final CategoriaRepository categorias;private final PedidoRepository pedidos;private final PagoRepository pagos;private final PlanService planes;private final ComisionAutomaticaService comisiones;private final PasswordEncoder encoder;private final UsuarioActual usuario;private final Clock reloj;
 @Value("${yapaa.demo.habilitado:false}") private boolean demo;
 public EscenarioDemoService(CuentaRepository c,RolRepository r,NegocioRepository n,NegocioPlanRepository v,PlanComercialRepository cat,ProductoRepository pr,CategoriaRepository ca,PedidoRepository pe,PagoRepository pa,PlanService pl,ComisionAutomaticaService co,PasswordEncoder en,UsuarioActual u,Clock clock){cuentas=c;roles=r;negocios=n;vinculos=v;catalogo=cat;productos=pr;categorias=ca;pedidos=pe;pagos=pa;planes=pl;comisiones=co;encoder=en;usuario=u;reloj=clock;}
 private static final String[] NOMBRES={"La Casona del Sabor","Panadería San Andrés","Café del Parque","Sazón de Casa","El Horno Artesanal","La Mesa Norteña","Bocados de Trujillo","Café Alameda","Pan y Tradición","La Esquina Criolla","Brasa del Valle","Dulces del Jardín","El Rincón del Pan","La Cocina de Julia","Café Los Cedros","Hamburguesas El Patio","Sabores de la Plaza","Pastelería La Vitrina","El Buen Sándwich","Panadería Santa Clara","La Terraza del Café","Antojos del Norte","La Huerta Cocina","Dulce Canela","El Patio de las Alitas","La Merienda","Café Primavera","La Casa del Croissant","Cocina El Molino","Los Sabores del Barrio"};
 private static final String[] DISTRITOS={"Trujillo","Víctor Larco Herrera","La Esperanza","Huanchaco","El Porvenir","Florencia de Mora"};
 @Transactional public synchronized Map<String,Object> crear(){
  if(!usuario.esAdministrador())throw new org.springframework.security.access.AccessDeniedException("Acceso restringido.");
  if(!demo)throw new ReglaNegocioException("El escenario solo esta disponible en demostracion.");
  LocalDate hoy=LocalDate.now(reloj);LocalDate inicio=hoy.withDayOfMonth(1);String hash=encoder.encode(UUID.randomUUID().toString()+UUID.randomUUID());
  planes.prepararCatalogo();var cantidadPlan=new HashMap<String,Integer>();for(var n:negocios.findAll()){String plan=planes.vigente(n.getIdNegocio()).getPlan().getNombre();cantidadPlan.merge(plan,1,Integer::sum);}
  Map<String,Integer> objetivo=Map.of("Normal",18,"Pro",9,"Promocional",3);int creados=0;
  for(int i=1;negocios.count()<30&&i<=100;i++){
   String ruc="20999"+String.format("%06d",i);if(negocios.existsByRuc(ruc))continue;
   var cuenta=cuenta("negocio"+i+"@escenario.yapaa.test","NEGOCIO",hash,null,i);
   var n=new Negocio();n.setCuenta(cuenta);n.setRuc(ruc);n.setNombreComercial(NOMBRES[(i-1)%NOMBRES.length]);n.setRazonSocial(n.getNombreComercial()+" S.A.C.");n.setTipoContribuyente("PERSONA_JURIDICA");n.setDireccion("Av. Los Jardines "+(100+i*13));n.setDistrito(DISTRITOS[(i-1)%DISTRITOS.length]);n.setCiudad("Trujillo");n.setTelefonoContacto("900700"+String.format("%03d",i));n.setEstadoAprobacion("APROBADO");n.setActivo(true);n.setFechaRegistro(inicio.atTime(8,0));negocios.saveAndFlush(n);
   String nombre=List.of("Normal","Pro","Promocional").stream().max(Comparator.comparingInt(p->objetivo.get(p)-cantidadPlan.getOrDefault(p,0))).orElse("Normal");
   var plan=catalogo.findByNombre(nombre).orElseThrow();var v=new NegocioPlan();v.setNegocio(n);v.setPlan(plan);v.setFechaInicio(inicio);v.setFechaFin(nombre.equals("Promocional")?inicio.plusMonths(1).minusDays(1):null);v.setPorcentajeAplicado(plan.getPorcentajeComision());v.setEstado("ACTIVO");vinculos.saveAndFlush(v);cantidadPlan.merge(nombre,1,Integer::sum);creados++;
  }
  List<Cuenta> compradores=new ArrayList<>();String[] nombres={"Ana","Luis","María","Jorge","Lucía","Carlos","Valeria","Diego"};
  for(int i=1;i<=40;i++)compradores.add(cuenta("cliente"+i+"@escenario.yapaa.test","CLIENTE",hash,nombres[(i-1)%nombres.length],i));
  var cats=categorias.findAll().stream().filter(c->Boolean.TRUE.equals(c.getActivo())).toList();if(cats.isEmpty())throw new ReglaNegocioException("Faltan categorias para crear el escenario.");
  int generadas=0;int indice=0;
  for(var n:negocios.findAll()){
   boolean nuevo=n.getRuc().startsWith("20999");boolean previo=Set.of("La Burger Trujillana Demo","Pan Calientito Demo","Cafeteria La Esquina Demo").contains(n.getNombreComercial());if(!nuevo&&!previo)continue;
   var lista=productos.findByNegocioIdNegocio(n.getIdNegocio(),Pageable.unpaged()).getContent();
   if(nuevo&&lista.isEmpty()){
    for(int j=0;j<3;j++)crearProducto(n,cats.get(indice%cats.size()),indice%4,j,hoy,inicio);
    lista=productos.findByNegocioIdNegocio(n.getIdNegocio(),Pageable.unpaged()).getContent();
   }
   if(lista.isEmpty())continue;
   int ventas=28+(indice*7)%19;
   for(int k=0;k<ventas;k++){
    String codigo="D30-"+n.getIdNegocio()+"-"+String.format("%03d",k);if(pedidos.existsByCodigoPedido(codigo))continue;
    Cuenta comprador=compradores.get((indice*5+k)%compradores.size());boolean delivery=(indice+k)%3==0;
    int dias=Math.min(hoy.getDayOfMonth()-1,(k*5+indice)%Math.min(21,hoy.getDayOfMonth()));
    LocalDateTime fecha=hoy.minusDays(dias).atTime(9+(k%11),(k*13)%60);if(fecha.isAfter(LocalDateTime.now(reloj)))fecha=LocalDateTime.now(reloj).minusMinutes(10+k);
    var pedido=new Pedido();pedido.setCuenta(comprador);pedido.setCodigoPedido(codigo);pedido.setFechaPedido(fecha);pedido.setEstadoPago("CONFIRMADO");pedido.setStockDescontado(false);pedido.setEstadoPedido(k%8==0?"CONFIRMADO":"COMPLETADO");
    var venta=new PedidoNegocio();venta.setPedido(pedido);venta.setNegocio(n);venta.setModalidadEntrega(delivery?"DELIVERY":"RECOJO");venta.setDireccionEntrega(delivery?"Jr. Las Flores "+(210+k)+", Trujillo":null);venta.setCostoDelivery(delivery?new BigDecimal(4+(k%4)):BigDecimal.ZERO);venta.setEstado(pedido.getEstadoPedido());
    BigDecimal subtotal=BigDecimal.ZERO;int lineas=1+(k%Math.min(3,lista.size()));
    for(int j=0;j<lineas;j++){
     var producto=lista.get((k+j)%lista.size());var d=new DetallePedido();d.setPedidoNegocio(venta);d.setProducto(producto);d.setNombreProducto(producto.getNombre());d.setCantidad(1+(k+j)%3);d.setPrecioUnitario(producto.getPrecioDescuento());d.setSubtotal(producto.getPrecioDescuento().multiply(BigDecimal.valueOf(d.getCantidad())));subtotal=subtotal.add(d.getSubtotal());venta.getDetalles().add(d);
    }
    venta.setSubtotalProductos(subtotal);venta.setTotal(subtotal.add(venta.getCostoDelivery()));pedido.getPedidosNegocio().add(venta);pedido.setSubtotalProductos(subtotal);pedido.setCostoDeliveryTotal(venta.getCostoDelivery());pedido.setTotal(venta.getTotal());pedidos.saveAndFlush(pedido);
    var pago=new Pago();pago.setPedido(pedido);pago.setCuentaRegistro(comprador);pago.setMetodoPago("TARJETA");pago.setMonto(pedido.getTotal());pago.setEstadoPago("CONFIRMADO");pago.setReferencia("SIM-D30-"+n.getIdNegocio()+"-"+k);pago.setFechaRegistro(fecha);pago.setFechaPago(fecha);pagos.saveAndFlush(pago);comisiones.aplicar(pedido,fecha);generadas++;
   }indice++;
  }
  return Map.of("negocios",negocios.count(),"nuevosNegocios",creados,"nuevasVentas",generadas,"planes",cantidadPlan);
 }
 private Cuenta cuenta(String correo,String rol,String hash,String nombre,int indice){
  var existente=cuentas.findByCorreo(correo);if(existente.isPresent())return existente.get();
  var c=new Cuenta();c.setCorreo(correo);c.setContrasenaHash(hash);c.setEstado("ACTIVO");c.getRoles().add(roles.findByNombre(rol).orElseThrow());
  if(nombre!=null){var cliente=new Cliente();cliente.setCuenta(c);cliente.setNombres(nombre);cliente.setApellidos(List.of("Ramos","Torres","Vega","Castro","Flores").get(indice%5));cliente.setTelefono("900800"+String.format("%03d",indice));c.setCliente(cliente);}return cuentas.saveAndFlush(c);
 }
 private void crearProducto(Negocio n,Categoria categoria,int tipo,int j,LocalDate hoy,LocalDate inicio){
  String[][] nombres={{"Hamburguesa clásica","Hamburguesa BBQ","Hamburguesa doble"},{"Alitas BBQ","Salchipapa personal","Papas crocantes"},{"Sándwich de pollo","Triple de la casa","Sándwich mixto"},{"Croissant artesanal","Pan integral","Empanada de pollo"}};
  String[][] precios={{"10.90","14.90","17.90"},{"16.90","9.90","7.50"},{"8.90","7.90","6.90"},{"4.50","6.50","4.90"}};
  String[][] fotos={{"hamburguesa","hamburguesa","hamburguesa"},{"alitas","papas","papas"},{"sandwich","sandwich","sandwich"},{"croissant","croissant-relleno","empanada"}};
  var p=new Producto();p.setNegocio(n);p.setCategoria(categoria);p.setNombre(nombres[tipo][j]);p.setDescripcion("Preparado en el día. Disponible hasta agotar existencias.");p.setImagenUrl("/imagenes/productos/"+fotos[tipo][j]+".jpg");var precio=new BigDecimal(precios[tipo][j]);p.setPrecioDescuento(precio);p.setPrecioNormal(precio.multiply(new BigDecimal("1.5")).setScale(2,RoundingMode.HALF_UP));p.setStock(8+(n.getIdNegocio()+j)%18);p.setFechaRegistro(inicio.atTime(8,0));p.setFechaPublicacion(LocalDateTime.now(reloj));p.setFechaLimiteVenta(LocalDateTime.now(reloj).plusHours(4+(n.getIdNegocio()+j)%36));p.setEstadoPublicacion("PUBLICADO");p.setActivo(true);productos.saveAndFlush(p);
 }
}
