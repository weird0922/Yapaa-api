package pe.upn.yapaa_api.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.upn.yapaa_api.security.UsuarioActual;
import pe.upn.yapaa_api.exception.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service
public class DashboardService {
 private final JdbcTemplate jdbc;private final UsuarioActual usuario;
 public DashboardService(JdbcTemplate j,UsuarioActual u){jdbc=j;usuario=u;}
 public record Resumen(long negocios,long aprobados,long pendientes,long clientes,long pedidos,long ventas,BigDecimal ventasProductos,BigDecimal delivery,BigDecimal totalCobrado,BigDecimal comisionYapaa,BigDecimal ingresoNegocios,List<Map<String,Object>> evolucion,List<Map<String,Object>> negociosResumen,List<Map<String,Object>> planes){}
 private record Periodo(java.sql.Timestamp inicio,java.sql.Timestamp fin){}
 private Periodo periodo(LocalDate desde,LocalDate hasta){if(!usuario.esAdministrador())throw new org.springframework.security.access.AccessDeniedException("Acceso restringido.");if(desde.isAfter(hasta)||desde.isBefore(hasta.minusYears(2)))throw new ReglaNegocioException("Selecciona un intervalo valido de hasta dos anos.");return new Periodo(java.sql.Timestamp.valueOf(desde.atStartOfDay()),java.sql.Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));}
 private static final String BASE=" FROM comision c JOIN pedido_negocio pn ON pn.id_pedido_negocio=c.id_pedido_negocio JOIN pedido p ON p.id_pedido=pn.id_pedido WHERE c.estado='LIQUIDADA' AND p.estado_pago='CONFIRMADO' AND pn.estado<>'CANCELADO' AND EXISTS(SELECT 1 FROM pago a WHERE a.id_pedido=p.id_pedido AND a.estado_pago='CONFIRMADO') AND c.fecha_registro>=? AND c.fecha_registro<? ";
 @Transactional(readOnly=true) public Resumen consultar(LocalDate desde,LocalDate hasta){
  var f=periodo(desde,hasta);Object[] args={f.inicio(),f.fin()};
  var tot=jdbc.queryForMap("SELECT COUNT(*) ventas,COUNT(DISTINCT p.id_pedido) pedidos,COALESCE(SUM(c.base_calculo),0) productos,COALESCE(SUM(pn.costo_delivery),0) delivery,COALESCE(SUM(c.base_calculo+pn.costo_delivery),0) total,COALESCE(SUM(c.monto_comision),0) comision"+BASE,args);
  var com=(BigDecimal)tot.get("comision");var total=(BigDecimal)tot.get("total");
  var negocios=jdbc.queryForList("WITH ventas AS (SELECT pn.id_negocio,COUNT(*) ventas,SUM(c.base_calculo) productos,SUM(pn.costo_delivery) delivery,SUM(c.monto_comision) comision"+BASE+" GROUP BY pn.id_negocio) SELECT n.id_negocio idNegocio,n.nombre_comercial nombre,n.estado_aprobacion estado,pc.nombre [plan],COALESCE(np.porcentaje_aplicado,0) porcentaje,COALESCE(v.ventas,0) ventas,COALESCE(v.productos,0) productos,COALESCE(v.delivery,0) delivery,COALESCE(v.productos,0)+COALESCE(v.delivery,0) total,COALESCE(v.comision,0) comision,COALESCE(v.productos,0)+COALESCE(v.delivery,0)-COALESCE(v.comision,0) neto FROM negocio n OUTER APPLY(SELECT TOP 1 x.* FROM negocio_plan x WHERE x.id_negocio=n.id_negocio AND x.estado='ACTIVO' ORDER BY x.fecha_inicio DESC,x.id_negocio_plan DESC) np LEFT JOIN plan_comercial pc ON pc.id_plan=np.id_plan LEFT JOIN ventas v ON v.id_negocio=n.id_negocio ORDER BY comision DESC,n.id_negocio",args);
  var planes=jdbc.queryForList("SELECT pc.nombre [plan],COUNT(*) cantidad FROM negocio n CROSS APPLY(SELECT TOP 1 x.id_plan FROM negocio_plan x WHERE x.id_negocio=n.id_negocio AND x.estado='ACTIVO' ORDER BY x.fecha_inicio DESC,x.id_negocio_plan DESC) np JOIN plan_comercial pc ON pc.id_plan=np.id_plan GROUP BY pc.nombre ORDER BY pc.nombre");
  return new Resumen(numero("SELECT COUNT(*) FROM negocio"),numero("SELECT COUNT(*) FROM negocio WHERE activo=1 AND estado_aprobacion='APROBADO'"),numero("SELECT COUNT(*) FROM negocio WHERE estado_aprobacion='PENDIENTE'"),numero("SELECT COUNT(*) FROM cuenta_rol cr JOIN rol r ON r.id_rol=cr.id_rol WHERE r.nombre='CLIENTE'"),((Number)tot.get("pedidos")).longValue(),((Number)tot.get("ventas")).longValue(),(BigDecimal)tot.get("productos"),(BigDecimal)tot.get("delivery"),total,com,total.subtract(com),jdbc.queryForList("SELECT CONVERT(varchar(10),c.fecha_registro,23) fecha,SUM(c.monto_comision) comision,SUM(c.base_calculo+pn.costo_delivery) ventas"+BASE+" GROUP BY CONVERT(varchar(10),c.fecha_registro,23) ORDER BY fecha",args),negocios,planes);
 }
 public record ProductoVenta(Integer idDetalle,Integer idProducto,String producto,int cantidad,BigDecimal precio,BigDecimal subtotal,BigDecimal comision,BigDecimal neto){}
 public record Venta(Integer idPedidoNegocio,String codigo,LocalDateTime fecha,String estado,String modalidad,BigDecimal productos,BigDecimal delivery,BigDecimal total,BigDecimal porcentaje,BigDecimal comision,BigDecimal neto,List<ProductoVenta> detalle){}
 public record DetalleNegocio(Integer idNegocio,String nombre,List<Venta> ventas,long totalElementos,int pagina,int totalPaginas,Map<String,Object> negocio,Map<String,Object> resumen){}
 @Transactional(readOnly=true) public DetalleNegocio detalle(Integer id,LocalDate desde,LocalDate hasta,int pagina){
  var f=periodo(desde,hasta);if(pagina<0||pagina>1000000)throw new ReglaNegocioException("Pagina invalida.");
  var nombres=jdbc.queryForList("SELECT nombre_comercial FROM negocio WHERE id_negocio=?",String.class,id);if(nombres.isEmpty())throw new RecursoNoEncontradoException("Negocio no encontrado.");
  long cantidad=jdbc.queryForObject("SELECT COUNT(*)"+BASE+" AND pn.id_negocio=?",Long.class,f.inicio(),f.fin(),id);
  var filas=jdbc.queryForList("SELECT pn.id_pedido_negocio id,p.codigo_pedido codigo,c.fecha_registro fecha,pn.estado,pn.modalidad_entrega modalidad,c.base_calculo productos,pn.costo_delivery delivery,pn.total,c.porcentaje_aplicado porcentaje,c.monto_comision comision"+BASE+" AND pn.id_negocio=? ORDER BY c.fecha_registro DESC,pn.id_pedido_negocio DESC OFFSET ? ROWS FETCH NEXT 10 ROWS ONLY",f.inicio(),f.fin(),id,Math.multiplyExact(pagina,10));
  List<Venta> ventas=new ArrayList<>();
  for(var row:filas){int ventaId=((Number)row.get("id")).intValue();var detalles=jdbc.queryForList("SELECT id_detalle,id_producto,nombre_producto,cantidad,precio_unitario,subtotal FROM detalle_pedido WHERE id_pedido_negocio=? ORDER BY id_detalle",ventaId);
   BigDecimal comision=(BigDecimal)row.get("comision"),total=(BigDecimal)row.get("total");var partes=RepartoComision.repartir(detalles.stream().map(d->(BigDecimal)d.get("subtotal")).toList(),comision);List<ProductoVenta> productos=new ArrayList<>();
   for(int i=0;i<detalles.size();i++){var d=detalles.get(i);var subtotal=(BigDecimal)d.get("subtotal");productos.add(new ProductoVenta(((Number)d.get("id_detalle")).intValue(),((Number)d.get("id_producto")).intValue(),(String)d.get("nombre_producto"),((Number)d.get("cantidad")).intValue(),(BigDecimal)d.get("precio_unitario"),subtotal,partes.get(i),subtotal.subtract(partes.get(i))));}
   ventas.add(new Venta(ventaId,(String)row.get("codigo"),((java.sql.Timestamp)row.get("fecha")).toLocalDateTime(),(String)row.get("estado"),(String)row.get("modalidad"),(BigDecimal)row.get("productos"),(BigDecimal)row.get("delivery"),total,(BigDecimal)row.get("porcentaje"),comision,total.subtract(comision),productos));
  }
  var negocio=jdbc.queryForMap("SELECT n.direccion,n.distrito,n.ciudad,n.ruc,n.telefono_contacto telefono,n.estado_aprobacion estado,pc.nombre [plan],COALESCE(np.porcentaje_aplicado,0) porcentaje FROM negocio n OUTER APPLY(SELECT TOP 1 * FROM negocio_plan x WHERE x.id_negocio=n.id_negocio AND x.estado='ACTIVO' ORDER BY fecha_inicio DESC,id_negocio_plan DESC) np LEFT JOIN plan_comercial pc ON pc.id_plan=np.id_plan WHERE n.id_negocio=?",id);
  var resumen=jdbc.queryForMap("SELECT COALESCE(SUM(c.base_calculo),0) productos,COALESCE(SUM(pn.costo_delivery),0) delivery,COALESCE(SUM(pn.total),0) total,COALESCE(SUM(c.monto_comision),0) comision,COALESCE(SUM(pn.total-c.monto_comision),0) neto"+BASE+" AND pn.id_negocio=?",f.inicio(),f.fin(),id);
  return new DetalleNegocio(id,nombres.getFirst(),ventas,cantidad,pagina,(int)((cantidad+9)/10),negocio,resumen);
 }
 private long numero(String sql){return jdbc.queryForObject(sql,Long.class);}
}
