package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import pe.upn.yapaa_api.repository.*;
import pe.upn.yapaa_api.model.*;
import pe.upn.yapaa_api.dto.plan.*;
import pe.upn.yapaa_api.exception.*;
import java.math.*;
import java.util.*;

@Service
public class VentaService {
    private final PedidoNegocioRepository ventas;
    private final PedidoRepository pedidos;
    private final ComisionRepository comisiones;
    private final ProductoRepository productos;
    private final PlanService planes;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    public VentaService(PedidoNegocioRepository v,PedidoRepository p,ComisionRepository c,ProductoRepository pr,PlanService pl){ventas=v;pedidos=p;comisiones=c;productos=pr;planes=pl;}
    @Transactional(readOnly=true)
    public Page<VentaResponse> listar(Integer id,Pageable pagina){planes.autorizar(id);return ventas.findByNegocioIdNegocio(id,pagina).map(this::respuesta);}
    @Transactional
    public VentaResponse confirmar(Integer id){
        var v=bloquear(id);planes.autorizar(v.getNegocio().getIdNegocio());
        if("CANCELADO".equals(v.getEstado()))throw new ReglaNegocioException("No puedes confirmar una venta cancelada.");
        if("COMPLETADO".equals(v.getEstado()))return respuesta(v);
        if(!"APROBADO".equals(v.getNegocio().getEstadoAprobacion()) || !Boolean.TRUE.equals(v.getNegocio().getActivo()))throw new ReglaNegocioException("El negocio no esta habilitado para confirmar ventas.");
        var plan=planes.vigente(v.getNegocio().getIdNegocio());
        if(comisiones.findByPedidoNegocioIdPedidoNegocio(id).isEmpty()){
            var c=new Comision();c.setPedidoNegocio(v);c.setBaseCalculo(v.getSubtotalProductos());
            c.setPorcentajeAplicado(plan.getPorcentajeAplicado());
            c.setMontoComision(v.getSubtotalProductos().multiply(plan.getPorcentajeAplicado()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP));
            c.setEstado("PENDIENTE");comisiones.saveAndFlush(c);
        }
        v.setEstado("COMPLETADO");ventas.save(v);actualizarPedido(v.getPedido());return respuesta(v);
    }
    @Transactional
    public VentaResponse cancelar(Integer id){
        var v=bloquear(id);planes.autorizar(v.getNegocio().getIdNegocio());
        if("CANCELADO".equals(v.getEstado()))return respuesta(v);
        if("COMPLETADO".equals(v.getEstado()) || comisiones.findByPedidoNegocioIdPedidoNegocio(id).isPresent())throw new ReglaNegocioException("Una venta realizada no puede cancelarse desde este flujo.");
        // Mismo orden de bloqueo de productos que la creacion del pedido.
        var detalles=new ArrayList<>(v.getDetalles());detalles.sort(Comparator.comparing(d->d.getProducto().getIdProducto()));
        for(var d:detalles){var p=productos.buscarParaActualizar(d.getProducto().getIdProducto()).orElseThrow();em.refresh(p,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);p.setStock(p.getStock()+d.getCantidad());productos.save(p);}
        v.setEstado("CANCELADO");ventas.save(v);actualizarPedido(v.getPedido());return respuesta(v);
    }
    private PedidoNegocio bloquear(Integer id){
        var previo=ventas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Pedido de negocio no encontrado."));
        planes.autorizar(previo.getNegocio().getIdNegocio());
        pedidos.bloquear(previo.getPedido().getIdPedido()).orElseThrow();
        var v=ventas.bloquear(id).orElseThrow();
        em.refresh(v,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return v;
    }
    private void actualizarPedido(Pedido pedido){
        var lista=pedido.getPedidosNegocio();
        if(lista.stream().allMatch(v->"CANCELADO".equals(v.getEstado())))pedido.setEstadoPedido("CANCELADO");
        else if(lista.stream().allMatch(v->Set.of("COMPLETADO","CANCELADO").contains(v.getEstado())))pedido.setEstadoPedido("COMPLETADO");
        pedidos.save(pedido);
    }
    @Transactional
    public ResumenVentas resumen(Integer id){
        planes.exigirPro(id);
        var lista=ventas.findByNegocioIdNegocioAndEstado(id,"COMPLETADO");
        BigDecimal bruto=BigDecimal.ZERO,comision=BigDecimal.ZERO;long unidades=0;
        for(var v:lista){bruto=bruto.add(v.getSubtotalProductos());unidades+=v.getDetalles().stream().mapToLong(DetallePedido::getCantidad).sum();
            comision=comision.add(comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio()).filter(c->!"ANULADA".equals(c.getEstado())).map(Comision::getMontoComision).orElse(BigDecimal.ZERO));}
        return new ResumenVentas(lista.size(),unidades,bruto,comision,bruto.subtract(comision));
    }
    @Transactional
    public String csv(Integer id){
        planes.exigirPro(id);
        StringBuilder s=new StringBuilder("pedido_negocio;fecha_pedido;subtotal_productos;delivery;comision;estado_comision\r\n");
        for(var v:ventas.findByNegocioIdNegocioAndEstado(id,"COMPLETADO")){
            var r=respuesta(v);s.append(r.idPedidoNegocio()).append(';').append(r.fechaPedido()).append(';').append(r.subtotalProductos()).append(';').append(r.costoDelivery()).append(';').append(r.comision()).append(';').append(r.estadoComision()).append("\r\n");
        }return s.toString();
    }
    private VentaResponse respuesta(PedidoNegocio v){
        var c=comisiones.findByPedidoNegocioIdPedidoNegocio(v.getIdPedidoNegocio());
        return new VentaResponse(v.getIdPedidoNegocio(),v.getPedido().getCodigoPedido(),v.getPedido().getFechaPedido(),v.getEstado(),v.getModalidadEntrega(),v.getSubtotalProductos(),v.getCostoDelivery(),v.getTotal(),v.getDetalles().stream().map(d->d.getCantidad()+" x "+d.getNombreProducto()).toList(),c.map(Comision::getPorcentajeAplicado).orElse(null),c.map(Comision::getMontoComision).orElse(BigDecimal.ZERO),c.map(Comision::getEstado).orElse("SIN GENERAR"));
    }
}
