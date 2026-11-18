package pe.upn.yapaa_api.dto.auth;

import java.util.List;

public class LoginResponse {

    private String token;
    private String tipo;
    private Integer idCuenta;
    private String correo;
    private String nombres;
    private List<String> roles;

    public LoginResponse() {
    }

    public LoginResponse(String token, Integer idCuenta, String correo, String nombres, List<String> roles) {
        this.token = token;
        this.tipo = "Bearer";
        this.idCuenta = idCuenta;
        this.correo = correo;
        this.nombres = nombres;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
