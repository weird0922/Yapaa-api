package pe.upn.yapaa_api.dto.plan;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record VentaResponse(Integer idPedidoNegocio,String codigoPedido,LocalDateTime fechaPedido,
    String estado,String modalidadEntrega,BigDecimal subtotalProductos,BigDecimal costoDelivery,
    BigDecimal total,List<String> productos,BigDecimal porcentajeComision,BigDecimal comision,String estadoComision) {}
