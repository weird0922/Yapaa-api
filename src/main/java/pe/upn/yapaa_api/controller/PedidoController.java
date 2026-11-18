package pe.upn.yapaa_api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.pedido.ConfirmarPagoRequest;
import pe.upn.yapaa_api.dto.pedido.PedidoRequest;
import pe.upn.yapaa_api.dto.pedido.PedidoResponse;
import pe.upn.yapaa_api.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<Result<PedidoResponse>> crear(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crear(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<PedidoResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(id));
    }

    @GetMapping("/cuenta/{idCuenta}")
    public ResponseEntity<Result<Page<PedidoResponse>>> listarPorCuenta(
            @PathVariable Integer idCuenta,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPorCuenta(idCuenta, pageable));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<Result<Page<PedidoResponse>>> listarPorEstado(
            @PathVariable String estado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPorEstado(estado, pageable));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Result<PedidoResponse>> cambiarEstado(
            @PathVariable Integer id, @RequestParam String estado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, estado));
    }
}
