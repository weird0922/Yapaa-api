package pe.upn.yapaa_api.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductoRequest {

    @NotNull(message = "El negocio es obligatorio.")
    private Integer idNegocio;

    @NotNull(message = "La categoria es obligatoria.")
    private Integer idCategoria;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 150, message = "El nombre no debe superar 150 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripcion no debe superar 500 caracteres.")
    private String descripcion;

    @Size(max = 500, message = "La URL de la imagen no debe superar 500 caracteres.")
    private String imagenUrl;

    @NotNull(message = "El precio normal es obligatorio.")
    @DecimalMin(value = "0.01", message = "El precio normal debe ser mayor a 0.")
    @Digits(integer = 8, fraction = 2, message = "El precio normal no tiene un formato valido.")
    private BigDecimal precioNormal;

    @NotNull(message = "El precio de descuento es obligatorio.")
    @DecimalMin(value = "0.01", message = "El precio de descuento debe ser mayor a 0.")
    @Digits(integer = 8, fraction = 2, message = "El precio de descuento no tiene un formato valido.")
    private BigDecimal precioDescuento;

    @NotNull(message = "El stock es obligatorio.")
    @Min(value = 0, message = "El stock no puede ser negativo.")
    private Integer stock;

    private LocalDateTime fechaPublicacion;

    @NotNull(message = "La fecha limite de venta es obligatoria.")
    private LocalDateTime fechaLimiteVenta;

    @Pattern(regexp = "BORRADOR|PUBLICADO|AGOTADO|FINALIZADO",
            message = "El estado de publicacion no es valido.")
    private String estadoPublicacion;

    public Integer getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(Integer idNegocio) {
        this.idNegocio = idNegocio;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getPrecioNormal() {
        return precioNormal;
    }

    public void setPrecioNormal(BigDecimal precioNormal) {
        this.precioNormal = precioNormal;
    }

    public BigDecimal getPrecioDescuento() {
        return precioDescuento;
    }

    public void setPrecioDescuento(BigDecimal precioDescuento) {
        this.precioDescuento = precioDescuento;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public LocalDateTime getFechaLimiteVenta() {
        return fechaLimiteVenta;
    }

    public void setFechaLimiteVenta(LocalDateTime fechaLimiteVenta) {
        this.fechaLimiteVenta = fechaLimiteVenta;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public void setEstadoPublicacion(String estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }
}
