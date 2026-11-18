package pe.upn.yapaa_api.dto.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class PedidoRequest {

    @NotEmpty(message = "El pedido debe tener al menos un grupo.")
    @Valid
    private List<GrupoPedidoRequest> grupos;

    public List<GrupoPedidoRequest> getGrupos() {
        return grupos;
    }

    public void setGrupos(List<GrupoPedidoRequest> grupos) {
        this.grupos = grupos;
    }
}
