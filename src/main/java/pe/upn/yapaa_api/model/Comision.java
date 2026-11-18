package pe.upn.yapaa_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comision")
public class Comision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comision")
    private Integer idComision;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido_negocio", nullable = false, unique = true)
    private PedidoNegocio pedidoNegocio;

    @Column(name = "base_calculo", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseCalculo;

    @Column(name = "porcentaje_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeAplicado;

    @Column(name = "monto_comision", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoComision;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "fecha_liquidacion")
    private LocalDateTime fechaLiquidacion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void antesDeInsertar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    public Integer getIdComision() {
        return idComision;
    }

    public void setIdComision(Integer idComision) {
        this.idComision = idComision;
    }

    public PedidoNegocio getPedidoNegocio() {
        return pedidoNegocio;
    }

    public void setPedidoNegocio(PedidoNegocio pedidoNegocio) {
        this.pedidoNegocio = pedidoNegocio;
    }

    public BigDecimal getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(BigDecimal baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public BigDecimal getPorcentajeAplicado() {
        return porcentajeAplicado;
    }

    public void setPorcentajeAplicado(BigDecimal porcentajeAplicado) {
        this.porcentajeAplicado = porcentajeAplicado;
    }

    public BigDecimal getMontoComision() {
        return montoComision;
    }

    public void setMontoComision(BigDecimal montoComision) {
        this.montoComision = montoComision;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(LocalDateTime fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
