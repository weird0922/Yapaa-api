package pe.upn.yapaa_api.common;

public class Result<T> {

    private Boolean exitoso;
    private String mensaje;
    private T datos;

    public Result() {
    }

    public Result(Boolean exitoso, String mensaje, T datos) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    public static <T> Result<T> exitoso(T datos) {
        return new Result<>(Boolean.TRUE, "Operacion exitosa.", datos);
    }

    public static <T> Result<T> exitoso(String mensaje, T datos) {
        return new Result<>(Boolean.TRUE, mensaje, datos);
    }

    public static <T> Result<T> error(String mensaje) {
        return new Result<>(Boolean.FALSE, mensaje, null);
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public T getDatos() {
        return datos;
    }

    public void setDatos(T datos) {
        this.datos = datos;
    }
}
