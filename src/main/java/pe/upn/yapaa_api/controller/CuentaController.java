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
import pe.upn.yapaa_api.dto.cuenta.CuentaResponse;
import pe.upn.yapaa_api.service.CuentaService;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<CuentaResponse>>> listar(
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(cuentaService.listar(estado, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<CuentaResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(cuentaService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Result<CuentaResponse>> cambiarEstado(
            @PathVariable Integer id, @RequestParam String estado) {
        return ResponseEntity.ok(cuentaService.cambiarEstado(id, estado));
    }
}
