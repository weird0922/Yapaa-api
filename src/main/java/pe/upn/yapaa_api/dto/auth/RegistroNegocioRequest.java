package pe.upn.yapaa_api.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.upn.yapaa_api.dto.negocio.AfiliacionRequest;
public class RegistroNegocioRequest extends AfiliacionRequest {
    @NotBlank(message="El correo de la cuenta de negocio es obligatorio.")
    @Email(message="Ingresa un correo valido.")
    private String correo;
    @NotBlank(message="La contrasena es obligatoria.")
    @Size(min=6, max=100, message="La contrasena debe tener entre 6 y 100 caracteres.")
    private String contrasena;
    public String getCorreo(){return correo;}
    public void setCorreo(String correo){this.correo=correo;}
    public String getContrasena(){return contrasena;}
    public void setContrasena(String contrasena){this.contrasena=contrasena;}
}
