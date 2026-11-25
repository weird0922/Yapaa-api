package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import java.time.*;
import java.math.*;
@Service
public class ComisionAutomaticaService {
 private final ComisionRepository comisiones;private final PlanService planes;
 public ComisionAutomaticaService(ComisionRepository c,PlanService p){comisiones=c;planes=p;}
 // El llamador mantiene el bloqueo del pedido y la transaccion del pago.
 @Transactional public void aplicar(Pedido pedido,LocalDateTime fecha){
  if(!"CONFIRMADO".equals(pedido.getEstadoPago()))throw new ReglaNegocioException("El pedido no tiene un pago aprobado.");
  for(var venta:pedido.getPedidosNegocio()){
   var existente=comisiones.findByPedidoNegocioIdPedidoNegocio(venta.getIdPedidoNegocio());
   if(existente.isPresent()){
    var c=existente.get();if("ANULADA".equals(c.getEstado()))throw new ReglaNegocioException("La venta fue anulada.");
    if(!"LIQUIDADA".equals(c.getEstado())){c.setEstado("LIQUIDADA");c.setFechaLiquidacion(fecha);comisiones.save(c);}continue;
   }
   var plan=planes.vigente(venta.getNegocio().getIdNegocio());
   var c=new Comision();c.setPedidoNegocio(venta);c.setBaseCalculo(venta.getSubtotalProductos());c.setPorcentajeAplicado(plan.getPorcentajeAplicado());
   c.setMontoComision(venta.getSubtotalProductos().multiply(plan.getPorcentajeAplicado()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP));
   c.setEstado("LIQUIDADA");c.setFechaRegistro(fecha);c.setFechaLiquidacion(fecha);comisiones.saveAndFlush(c);
  }
 }
 @Transactional public void revertir(Pedido pedido){for(var venta:pedido.getPedidosNegocio())comisiones.findByPedidoNegocioIdPedidoNegocio(venta.getIdPedidoNegocio()).ifPresent(c->{c.setEstado("ANULADA");c.setFechaLiquidacion(null);comisiones.save(c);});}
}
