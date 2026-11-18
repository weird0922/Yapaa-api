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
@Table(name = "pedido_negocio")
public class PedidoNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido_negocio")
    private Integer idPedidoNegocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_negocio", nullable = false)
    private Negocio negocio;

    @Column(name = "modalidad_entrega", nullable = false, length = 10)
    private String modalidadEntrega;

    @Column(name = "direccion_entrega", length = 250)
    private String direccionEntrega;

    @Column(name = "costo_delivery", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoDelivery;

    @Column(name = "subtotal_productos", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalProductos;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    @Column(name = "fecha_recojo_programada")
    private LocalDateTime fechaRecojoProgramada;

    @OneToMany(mappedBy = "pedidoNegocio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetallePedido> detalles = new ArrayList<>();

    @PrePersist
    public void antesDeInsertar() {
        if (estado == null) {
            estado = "PENDIENTE";
        }
        if (costoDelivery == null) {
            costoDelivery = BigDecimal.ZERO;
        }
    }

    public Integer getIdPedidoNegocio() {
        return idPedidoNegocio;
    }

    public void setIdPedidoNegocio(Integer idPedidoNegocio) {
        this.idPedidoNegocio = idPedidoNegocio;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public void setNegocio(Negocio negocio) {
        this.negocio = negocio;
    }

    public String getModalidadEntrega() {
        return modalidadEntrega;
    }

    public void setModalidadEntrega(String modalidadEntrega) {
        this.modalidadEntrega = modalidadEntrega;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public BigDecimal getCostoDelivery() {
        return costoDelivery;
    }

    public void setCostoDelivery(BigDecimal costoDelivery) {
        this.costoDelivery = costoDelivery;
    }

    public BigDecimal getSubtotalProductos() {
        return subtotalProductos;
    }

    public void setSubtotalProductos(BigDecimal subtotalProductos) {
        this.subtotalProductos = subtotalProductos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRecojoProgramada() {
        return fechaRecojoProgramada;
    }

    public void setFechaRecojoProgramada(LocalDateTime fechaRecojoProgramada) {
        this.fechaRecojoProgramada = fechaRecojoProgramada;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }
}
