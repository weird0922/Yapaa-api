package pe.upn.yapaa_api.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PedidoNegocioResponse {

    private Integer idPedidoNegocio;
    private Integer idNegocio;
    private String nombreNegocio;
    private String telefonoContacto;
    private String modalidadEntrega;
    private String direccionEntrega;
    private BigDecimal costoDelivery;
    private BigDecimal subtotalProductos;
    private BigDecimal total;
    private String estado;
    private LocalDateTime fechaRecojoProgramada;
    private String mensaje;
    private String enlaceWhatsApp;
    private List<DetallePedidoResponse> detalles;

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

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
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

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEnlaceWhatsApp() {
        return enlaceWhatsApp;
    }

    public void setEnlaceWhatsApp(String enlaceWhatsApp) {
        this.enlaceWhatsApp = enlaceWhatsApp;
    }

    public List<DetallePedidoResponse> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedidoResponse> detalles) {
        this.detalles = detalles;
    }
}
