package pe.upn.yapaa_api.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PedidoResponse {

    private Integer idPedido;
    private String codigoPedido;
    private LocalDateTime fechaPedido;
    private Integer idCuenta;
    private String correoCliente;
    private BigDecimal subtotalProductos;
    private BigDecimal costoDeliveryTotal;
    private BigDecimal total;
    private String estadoPedido;
    private String estadoPago;
    private List<PedidoNegocioResponse> pedidosNegocio = new ArrayList<>();

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public String getCodigoPedido() {
        return codigoPedido;
    }

    public void setCodigoPedido(String codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    public BigDecimal getSubtotalProductos() {
        return subtotalProductos;
    }

    public void setSubtotalProductos(BigDecimal subtotalProductos) {
        this.subtotalProductos = subtotalProductos;
    }

    public BigDecimal getCostoDeliveryTotal() {
        return costoDeliveryTotal;
    }

    public void setCostoDeliveryTotal(BigDecimal costoDeliveryTotal) {
        this.costoDeliveryTotal = costoDeliveryTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(String estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public List<PedidoNegocioResponse> getPedidosNegocio() {
        return pedidosNegocio;
    }

    public void setPedidosNegocio(List<PedidoNegocioResponse> pedidosNegocio) {
        this.pedidosNegocio = pedidosNegocio;
    }
}
