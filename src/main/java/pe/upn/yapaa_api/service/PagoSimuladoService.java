package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.security.UsuarioActual;
import pe.upn.yapaa_api.exception.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service
public class PagoSimuladoService {
 public record Respuesta(Integer idPedido,Integer idPago,String estado,BigDecimal monto,String referencia,boolean simulado){}
 private final PedidoRepository pedidos;private final PagoRepository pagos;private final UsuarioActual usuario;private final ComisionAutomaticaService comisiones;private final ProductoRepository productos;
 @PersistenceContext private EntityManager em;
 public PagoSimuladoService(PedidoRepository p,PagoRepository a,UsuarioActual u,ComisionAutomaticaService c,ProductoRepository pr){pedidos=p;pagos=a;usuario=u;comisiones=c;productos=pr;}
 private Pedido bloquear(Integer id){
  if(!usuario.tieneRol("CLIENTE")||usuario.tieneRol("NEGOCIO"))throw new org.springframework.security.access.AccessDeniedException("Solo clientes pueden realizar esta operacion.");
  var p=pedidos.bloquear(id).orElseThrow(()->new RecursoNoEncontradoException("Pedido no encontrado."));em.refresh(p,LockModeType.PESSIMISTIC_WRITE);usuario.verificarPedido(p);return p;
 }
 @Transactional public Respuesta simular(Integer id,String escenario){
  var pedido=bloquear(id);
  if(!Set.of("APROBADO","RECHAZADO").contains(escenario))throw new ReglaNegocioException("Selecciona un resultado valido.");
  var aprobado=pagos.findByPedidoIdPedido(id,org.springframework.data.domain.Pageable.unpaged()).stream().filter(p->"CONFIRMADO".equals(p.getEstadoPago())&&p.getReferencia()!=null&&p.getReferencia().startsWith("SIM-")).findFirst();
  if(aprobado.isPresent())return respuesta(aprobado.get());
  if(pedido.getPedidosNegocio().stream().anyMatch(v->Set.of("CANCELADO","COMPLETADO").contains(v.getEstado())))throw new ReglaNegocioException("Este pedido ya fue finalizado.");
  if("CONFIRMADO".equals(pedido.getEstadoPago()))throw new ReglaNegocioException("Este pedido ya tiene un pago registrado.");
  if(pagos.countByPedidoIdPedido(id)>=5)throw new ReglaNegocioException("Se alcanzo el limite de intentos para este pedido.");
  // Comprobar que cada local sigue habilitado antes de aceptar el cobro.
  for(var v:pedido.getPedidosNegocio())if(!Boolean.TRUE.equals(v.getNegocio().getActivo())||!"APROBADO".equals(v.getNegocio().getEstadoAprobacion()))throw new ReglaNegocioException("Uno de los negocios no esta disponible.");
  var pago=new Pago();pago.setPedido(pedido);pago.setCuentaRegistro(usuario.obtenerCuenta());pago.setMetodoPago("TARJETA");pago.setMonto(pedido.getTotal());pago.setReferencia("SIM-"+UUID.randomUUID());pago.setEstadoPago("APROBADO".equals(escenario)?"CONFIRMADO":"RECHAZADO");
  if("CONFIRMADO".equals(pago.getEstadoPago())){
   confirmarStock(pedido);
   var fecha=LocalDateTime.now();pago.setFechaPago(fecha);pedido.setEstadoPago("CONFIRMADO");pedido.setEstadoPedido("CONFIRMADO");pedido.getPedidosNegocio().forEach(v->v.setEstado("CONFIRMADO"));pedidos.saveAndFlush(pedido);
   comisiones.aplicar(pedido,fecha);
  }
  pagos.saveAndFlush(pago);return respuesta(pago);
 }
 private void confirmarStock(Pedido pedido) {
  var detalles = pedido.getPedidosNegocio().stream()
      .flatMap(venta -> venta.getDetalles().stream())
      .sorted(Comparator.comparing(detalle -> detalle.getProducto().getIdProducto()))
      .toList();
  var productosBloqueados = new LinkedHashMap<Producto, Integer>();
  for (var detalle : detalles) {
   var producto = productos.buscarParaActualizar(detalle.getProducto().getIdProducto())
       .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado."));
   em.refresh(producto, LockModeType.PESSIMISTIC_WRITE);
   productosBloqueados.merge(producto, detalle.getCantidad(), Integer::sum);
  }
  var limite = LocalDateTime.now().plusMinutes(1);
  for (var linea : productosBloqueados.entrySet()) {
   var producto = linea.getKey();
   if (pedido.tieneStockDescontado()) {
    PedidoService.validarVigencia(producto, limite);
   } else {
    PedidoService.validarDisponible(producto, linea.getValue(), limite);
    producto.setStock(producto.getStock() - linea.getValue());
   }
  }
  pedido.setStockDescontado(true);
 }

 @Transactional public Respuesta anular(Integer id){
  var pedido=bloquear(id);
  if(pedido.getPedidosNegocio().stream().anyMatch(v->"COMPLETADO".equals(v.getEstado())))throw new ReglaNegocioException("No se puede cancelar un pedido ya entregado.");
  var lista=pagos.findByPedidoIdPedido(id,org.springframework.data.domain.Pageable.unpaged()).stream().filter(p->p.getReferencia()!=null&&p.getReferencia().startsWith("SIM-")).sorted(Comparator.comparing(Pago::getIdPago).reversed()).toList();
  var pago=lista.stream().filter(p->"CONFIRMADO".equals(p.getEstadoPago())).findFirst().orElseGet(()->lista.stream().filter(p->"ANULADO".equals(p.getEstadoPago())).findFirst().orElseThrow(()->new ReglaNegocioException("No hay un pago aprobado para cancelar.")));
  if("ANULADO".equals(pago.getEstadoPago()))return respuesta(pago);
  var detalles=pedido.getPedidosNegocio().stream().filter(v->!"CANCELADO".equals(v.getEstado())).flatMap(v->v.getDetalles().stream()).sorted(Comparator.comparing(d->d.getProducto().getIdProducto())).toList();
  if (pedido.tieneStockDescontado()) for(var d:detalles){var p=productos.buscarParaActualizar(d.getProducto().getIdProducto()).orElseThrow();em.refresh(p,LockModeType.PESSIMISTIC_WRITE);p.setStock(p.getStock()+d.getCantidad());productos.save(p);}
  pago.setEstadoPago("ANULADO");pagos.save(pago);comisiones.revertir(pedido);pedido.setEstadoPago("REEMBOLSADO");pedido.setEstadoPedido("CANCELADO");pedido.getPedidosNegocio().forEach(v->v.setEstado("CANCELADO"));pedidos.save(pedido);return respuesta(pago);
 }
 private Respuesta respuesta(Pago pago){return new Respuesta(pago.getPedido().getIdPedido(),pago.getIdPago(),pago.getEstadoPago(),pago.getMonto(),pago.getReferencia(),true);}
}
