package pe.upn.yapaa_api.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "El correo o usuario es obligatorio.")
    @Pattern(regexp = "(?i)(cliente|negocio|admin|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)", message = "Ingresa un correo valido o un usuario de demostracion.")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria.")
    private String contrasena;

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
}
