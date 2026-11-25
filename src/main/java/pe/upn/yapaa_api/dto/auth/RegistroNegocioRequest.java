package pe.upn.yapaa_api.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.upn.yapaa_api.dto.negocio.AfiliacionRequest;
public class RegistroNegocioRequest extends AfiliacionRequest {
    private String confirmarContrasena;
    private String tokenVerificacion;
    public String getConfirmarContrasena(){return confirmarContrasena;}
    public void setConfirmarContrasena(String v){confirmarContrasena=v;}
    public String getTokenVerificacion(){return tokenVerificacion;}
    public void setTokenVerificacion(String v){tokenVerificacion=v;}
    @NotBlank(message="El correo de la cuenta de negocio es obligatorio.")
    @Email(message="Ingresa un correo valido.")
    private String correo;
    @NotBlank(message="La contrasena es obligatoria.")
    @Size(min=10, max=64, message="Usa entre 10 y 64 caracteres.")
    private String contrasena;
    public String getCorreo(){return correo;}
    public void setCorreo(String correo){this.correo=correo;}
    public String getContrasena(){return contrasena;}
    public void setContrasena(String contrasena){this.contrasena=contrasena;}
}
