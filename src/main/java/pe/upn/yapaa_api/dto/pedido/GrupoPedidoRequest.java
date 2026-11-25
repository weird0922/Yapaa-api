package pe.upn.yapaa_api.dto.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public class GrupoPedidoRequest {

    @NotNull(message = "El negocio es obligatorio.")
    private Integer idNegocio;

    @NotBlank(message = "La modalidad de entrega es obligatoria.")
    @Pattern(regexp = "RECOJO|DELIVERY", message = "La modalidad debe ser RECOJO o DELIVERY.")
    private String modalidadEntrega;

    @jakarta.validation.constraints.Size(max=250)
    private String direccionEntrega;

    @DecimalMin(value = "0.00", message = "El costo de delivery no puede ser negativo.")
    @jakarta.validation.constraints.Digits(integer=4,fraction=2)
    @jakarta.validation.constraints.DecimalMax("999.99")
    private BigDecimal costoDelivery;

    @NotEmpty(message = "El grupo debe tener al menos un producto.")
    @Valid
    @jakarta.validation.constraints.Size(max=50)
    private List<ItemProductoRequest> items;

    @jakarta.validation.constraints.Size(max=36)
    private String cotizacionDelivery;
    public String getCotizacionDelivery(){return cotizacionDelivery;}
    public void setCotizacionDelivery(String v){cotizacionDelivery=v;}

    public Integer getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(Integer idNegocio) {
        this.idNegocio = idNegocio;
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

    public List<ItemProductoRequest> getItems() {
        return items;
    }

    public void setItems(List<ItemProductoRequest> items) {
        this.items = items;
    }
}
