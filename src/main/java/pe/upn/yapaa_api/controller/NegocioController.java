package pe.upn.yapaa_api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.negocio.NegocioRequest;
import pe.upn.yapaa_api.service.ConsultaRucService;
import pe.upn.yapaa_api.dto.negocio.NegocioResponse;
import pe.upn.yapaa_api.service.NegocioService;

@RestController
@RequestMapping("/api/negocios")
public class NegocioController {

    private final NegocioService negocioService;
    private final ConsultaRucService consultaRuc;

    public NegocioController(NegocioService negocioService, ConsultaRucService consultaRuc) {
        this.negocioService = negocioService;
        this.consultaRuc = consultaRuc;
    }

    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<Result<pe.upn.yapaa_api.dto.negocio.ConsultaRucResponse>> consultarRuc(@PathVariable String ruc) {
        return ResponseEntity.ok(Result.exitoso(consultaRuc.consultar(ruc)));
    }

    @GetMapping("/mios")
    public ResponseEntity<Result<Page<NegocioResponse>>> mios(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(negocioService.misNegocios(pageable));
    }

    @GetMapping
    public ResponseEntity<Result<Page<NegocioResponse>>> listar(
            @RequestParam(required = false) String estadoAprobacion,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(negocioService.listar(estadoAprobacion, pageable));
    }

    @GetMapping("/cuenta/{idCuenta}")
    public ResponseEntity<Result<Page<NegocioResponse>>> listarPorCuenta(
            @PathVariable Integer idCuenta,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(negocioService.listarPorCuenta(idCuenta, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<NegocioResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(negocioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Result<NegocioResponse>> crear(@Valid @RequestBody NegocioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(negocioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<NegocioResponse>> actualizar(
            @PathVariable Integer id, @Valid @RequestBody NegocioRequest request) {
        return ResponseEntity.ok(negocioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/aprobacion")
    public ResponseEntity<Result<NegocioResponse>> cambiarAprobacion(
            @PathVariable Integer id, @RequestParam String estado) {
        return ResponseEntity.ok(negocioService.cambiarEstadoAprobacion(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> darDeBaja(@PathVariable Integer id) {
        return ResponseEntity.ok(negocioService.darDeBaja(id));
    }
}
