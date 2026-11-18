package pe.upn.yapaa_api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.pago.PagoResponse;
import pe.upn.yapaa_api.service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<PagoResponse>>> listar(
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(pagoService.listar(estado, pageable));
    }

    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<Result<Page<PagoResponse>>> listarPorPedido(
            @PathVariable Integer idPedido,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(pagoService.listarPorPedido(idPedido, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<PagoResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Result<PagoResponse>> cambiarEstado(
            @PathVariable Integer id, @RequestParam String estado) {
        return ResponseEntity.ok(pagoService.cambiarEstado(id, estado));
    }
}
