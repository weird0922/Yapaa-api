package pe.upn.yapaa_api.dto.negocio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AfiliacionRequest {


    @NotBlank(message = "El nombre comercial es obligatorio.")
    @Size(max = 150, message = "El nombre comercial no debe superar 150 caracteres.")
    private String nombreComercial;

    @Size(max = 200, message = "La razon social no debe superar 200 caracteres.")
    private String razonSocial;

    @NotBlank(message = "El tipo de contribuyente es obligatorio.")
    @Pattern(regexp = "PERSONA_NATURAL|PERSONA_JURIDICA",
            message = "El tipo de contribuyente debe ser PERSONA_NATURAL o PERSONA_JURIDICA.")
    private String tipoContribuyente;

    @NotBlank(message = "El RUC es obligatorio.")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 digitos.")
    private String ruc;

    @NotBlank(message = "La direccion es obligatoria.")
    @Size(max = 250, message = "La direccion no debe superar 250 caracteres.")
    private String direccion;

    @NotBlank(message = "El distrito es obligatorio.")
    @Size(max = 100, message = "El distrito no debe superar 100 caracteres.")
    private String distrito;

    @NotBlank(message = "La ciudad es obligatoria.")
    @Size(max = 100, message = "La ciudad no debe superar 100 caracteres.")
    private String ciudad;

    @NotBlank(message = "El telefono de contacto es obligatorio.")
    @Size(max = 20, message = "El telefono no debe superar 20 caracteres.")
    private String telefonoContacto;

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getTipoContribuyente() {
        return tipoContribuyente;
    }

    public void setTipoContribuyente(String tipoContribuyente) {
        this.tipoContribuyente = tipoContribuyente;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }
}
