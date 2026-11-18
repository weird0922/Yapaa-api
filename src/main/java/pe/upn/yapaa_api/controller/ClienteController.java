package pe.upn.yapaa_api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.cliente.ClienteRequest;
import pe.upn.yapaa_api.dto.cliente.ClienteResponse;
import pe.upn.yapaa_api.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<ClienteResponse>>> listar(
            @RequestParam(required = false) String filtro,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(clienteService.listar(filtro, pageable));
    }

    @GetMapping("/{idCuenta}")
    public ResponseEntity<Result<ClienteResponse>> obtener(@PathVariable Integer idCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorId(idCuenta));
    }

    @PutMapping("/{idCuenta}")
    public ResponseEntity<Result<ClienteResponse>> actualizar(
            @PathVariable Integer idCuenta, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(idCuenta, request));
    }
}
