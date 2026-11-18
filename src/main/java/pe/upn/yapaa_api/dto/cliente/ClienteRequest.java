package pe.upn.yapaa_api.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteRequest {

    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(max = 100, message = "Los nombres no deben superar 100 caracteres.")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios.")
    @Size(max = 100, message = "Los apellidos no deben superar 100 caracteres.")
    private String apellidos;

    @NotBlank(message = "El telefono es obligatorio.")
    @Size(max = 20, message = "El telefono no debe superar 20 caracteres.")
    private String telefono;

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
