package pe.upn.yapaa_api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroRequest {

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato valido.")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria.")
    @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres.")
    private String contrasena;

    @Size(max = 100, message = "Los nombres no deben superar 100 caracteres.")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no deben superar 100 caracteres.")
    private String apellidos;

    @Size(max = 20, message = "El telefono no debe superar 20 caracteres.")
    private String telefono;

    @Pattern(regexp = "CLIENTE|NEGOCIO|cliente|negocio",
            message = "El rol debe ser CLIENTE o NEGOCIO.")
    private String rol;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
