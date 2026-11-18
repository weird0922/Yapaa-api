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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.producto.ProductoRequest;
import pe.upn.yapaa_api.dto.producto.ProductoResponse;
import pe.upn.yapaa_api.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/mios")
    public ResponseEntity<Result<Page<ProductoResponse>>> mios(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productoService.misProductos(pageable));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<Result<Page<ProductoResponse>>> listarDisponibles(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productoService.listarDisponibles(pageable));
    }

    @GetMapping("/negocio/{idNegocio}")
    public ResponseEntity<Result<Page<ProductoResponse>>> listarPorNegocio(
            @PathVariable Integer idNegocio,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productoService.listarPorNegocio(idNegocio, pageable));
    }

    @GetMapping("/categoria/{idCategoria}")
    public ResponseEntity<Result<Page<ProductoResponse>>> listarPorCategoria(
            @PathVariable Integer idCategoria,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productoService.listarPorCategoria(idCategoria, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<ProductoResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Result<ProductoResponse>> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<ProductoResponse>> actualizar(
            @PathVariable Integer id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<Result<ProductoResponse>> cambiarActivo(@PathVariable Integer id, @RequestParam boolean activo) {
        return ResponseEntity.ok(productoService.cambiarActivo(id,activo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> darDeBaja(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.darDeBaja(id));
    }
}
