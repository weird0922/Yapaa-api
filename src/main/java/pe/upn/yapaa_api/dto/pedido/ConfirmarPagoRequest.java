package pe.upn.yapaa_api.dto.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ConfirmarPagoRequest {

    @NotBlank(message = "El metodo de pago es obligatorio.")
    @Pattern(regexp = "EFECTIVO|TRANSFERENCIA|YAPE|PLIN|TARJETA|OTRO",
            message = "El metodo de pago no es valido.")
    private String metodoPago;

    private String referencia;

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
