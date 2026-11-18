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
import pe.upn.yapaa_api.dto.comision.ComisionResponse;
import pe.upn.yapaa_api.service.ComisionService;

@RestController
@RequestMapping("/api/comisiones")
public class ComisionController {

    private final ComisionService comisionService;

    public ComisionController(ComisionService comisionService) {
        this.comisionService = comisionService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<ComisionResponse>>> listar(
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(comisionService.listar(estado, pageable));
    }

    @GetMapping("/negocio/{idNegocio}")
    public ResponseEntity<Result<Page<ComisionResponse>>> listarPorNegocio(
            @PathVariable Integer idNegocio,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(comisionService.listarPorNegocio(idNegocio, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<ComisionResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(comisionService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/liquidar")
    public ResponseEntity<Result<ComisionResponse>> liquidar(@PathVariable Integer id) {
        return ResponseEntity.ok(comisionService.liquidar(id));
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<Result<ComisionResponse>> anular(@PathVariable Integer id) {
        return ResponseEntity.ok(comisionService.anular(id));
    }
}
