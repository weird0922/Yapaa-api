package pe.upn.yapaa_api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta", nullable = false)
    private Cuenta cuenta;

    @Column(name = "codigo_pedido", nullable = false, unique = true, length = 30)
    private String codigoPedido;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido;

    @Column(name = "subtotal_productos", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalProductos;

    @Column(name = "costo_delivery_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoDeliveryTotal;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "estado_pedido", nullable = false, length = 15)
    private String estadoPedido;

    @Column(name = "estado_pago", nullable = false, length = 12)
    private String estadoPago;

    @Column(name = "stock_descontado")
    private Boolean stockDescontado;

    public boolean tieneStockDescontado() {
        // Los pedidos históricos ya descontaban al crearse.
        return !Boolean.FALSE.equals(stockDescontado);
    }

    public void setStockDescontado(boolean stockDescontado) {
        this.stockDescontado = stockDescontado;
    }

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PedidoNegocio> pedidosNegocio = new ArrayList<>();

    @PrePersist
    public void antesDeInsertar() {
        if (fechaPedido == null) {
            fechaPedido = LocalDateTime.now();
        }
        if (estadoPedido == null) {
            estadoPedido = "PENDIENTE";
        }
        if (estadoPago == null) {
            estadoPago = "PENDIENTE";
        }
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public void setCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
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

    public List<PedidoNegocio> getPedidosNegocio() {
        return pedidosNegocio;
    }

    public void setPedidosNegocio(List<PedidoNegocio> pedidosNegocio) {
        this.pedidosNegocio = pedidosNegocio;
    }
}
