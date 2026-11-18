package pe.upn.yapaa_api.dto.comision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ComisionResponse {

    private Integer idComision;
    private Integer idPedidoNegocio;
    private Integer idNegocio;
    private String nombreNegocio;
    private BigDecimal baseCalculo;
    private BigDecimal porcentajeAplicado;
    private BigDecimal montoComision;
    private String estado;
    private LocalDateTime fechaLiquidacion;
    private LocalDateTime fechaRegistro;

    public Integer getIdComision() {
        return idComision;
    }

    public void setIdComision(Integer idComision) {
        this.idComision = idComision;
    }

    public Integer getIdPedidoNegocio() {
        return idPedidoNegocio;
    }

    public void setIdPedidoNegocio(Integer idPedidoNegocio) {
        this.idPedidoNegocio = idPedidoNegocio;
    }

    public Integer getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(Integer idNegocio) {
        this.idNegocio = idNegocio;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public void setNombreNegocio(String nombreNegocio) {
        this.nombreNegocio = nombreNegocio;
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
