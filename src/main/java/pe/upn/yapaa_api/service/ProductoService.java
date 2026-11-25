package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pe.upn.yapaa_api.security.UsuarioActual;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.producto.ProductoRequest;
import pe.upn.yapaa_api.dto.producto.ProductoResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Categoria;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.model.Producto;
import pe.upn.yapaa_api.repository.CategoriaRepository;
import pe.upn.yapaa_api.repository.NegocioRepository;
import pe.upn.yapaa_api.repository.ProductoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final UsuarioActual usuarioActual;
    private final NegocioRepository negocioRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           NegocioRepository negocioRepository,
                           CategoriaRepository categoriaRepository, UsuarioActual usuarioActual) {
        this.productoRepository = productoRepository;
        this.usuarioActual = usuarioActual;
        this.negocioRepository = negocioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Result<Page<ProductoResponse>> misProductos(Pageable pageable) {
        return Result.exitoso(productoRepository.findByNegocioCuentaIdCuenta(usuarioActual.obtenerCuenta().getIdCuenta(), pageable).map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public Result<Page<ProductoResponse>> listarDisponibles(Pageable pageable) {
        Page<ProductoResponse> pagina = productoRepository.buscarDisponibles(pageable)
                .map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<Page<ProductoResponse>> listarPorNegocio(Integer idNegocio, Pageable pageable) {
        Negocio negocio = negocioRepository.findById(idNegocio)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado."));
        boolean propietario = usuarioActual.esAdministrador() || usuarioActual.esPropietario(negocio.getCuenta().getIdCuenta());
        Page<ProductoResponse> pagina = (propietario
                ? productoRepository.findByNegocioIdNegocio(idNegocio, pageable)
                : productoRepository.buscarPublicadosPorNegocio(idNegocio, pageable)).map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<Page<ProductoResponse>> listarPorCategoria(Integer idCategoria, Pageable pageable) {
        Page<ProductoResponse> pagina = productoRepository
                .buscarDisponiblesPorCategoria(idCategoria, pageable)
                .map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<ProductoResponse> obtenerPorId(Integer id) {
        Producto producto = buscarPorId(id);
        if (!"PUBLICADO".equals(producto.getEstadoPublicacion()) || !Boolean.TRUE.equals(producto.getActivo())
                || !"APROBADO".equals(producto.getNegocio().getEstadoAprobacion()) || !Boolean.TRUE.equals(producto.getNegocio().getActivo())) {
            usuarioActual.verificarNegocio(producto.getNegocio());
        }
        return Result.exitoso(aRespuesta(producto));
    }

    @Transactional
    public Result<ProductoResponse> crear(ProductoRequest request) {
        validarPrecios(request.getPrecioNormal(), request.getPrecioDescuento());
        String imagen=request.getImagenUrl();
        if(imagen!=null&&!imagen.isBlank()&&!imagen.matches("(?i)https://[^\\s]+")&&!imagen.matches("/imagenes/productos/[a-zA-Z0-9._-]+"))throw new ReglaNegocioException("La fotografia debe usar una URL HTTPS o una imagen local del catalogo.");
        request.setNombre(request.getNombre().strip());
        if(request.getNombre().length()<2||request.getNombre().chars().anyMatch(Character::isISOControl))throw new ReglaNegocioException("Ingresa un nombre de producto valido.");
        if(request.getFechaLimiteVenta().isAfter(LocalDateTime.now().plusDays(30)))throw new ReglaNegocioException("El plazo de venta no puede superar los 30 dias.");
        if (request.getFechaLimiteVenta().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha limite de venta debe ser posterior al momento actual.");
        }
        if (productoRepository.existsByNegocioIdNegocioAndNombreIgnoreCase(
                request.getIdNegocio(), request.getNombre())) {
            throw new ReglaNegocioException("Ya existe un producto con ese nombre en el negocio.");
        }

        Negocio negocio = negocioRepository.findById(request.getIdNegocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado."));
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada."));

        usuarioActual.verificarNegocio(negocio);
        Producto producto = new Producto();
        producto.setNegocio(negocio);
        producto.setCategoria(categoria);
        aplicarDatos(producto, request);
        producto.setActivo(Boolean.TRUE);

        Producto guardado = productoRepository.save(producto);
        return Result.exitoso("Producto registrado correctamente.", aRespuesta(guardado));
    }

    @Transactional
    public Result<ProductoResponse> actualizar(Integer id, ProductoRequest request) {
        validarPrecios(request.getPrecioNormal(), request.getPrecioDescuento());
        String imagen=request.getImagenUrl();
        if(imagen!=null&&!imagen.isBlank()&&!imagen.matches("(?i)https://[^\\s]+")&&!imagen.matches("/imagenes/productos/[a-zA-Z0-9._-]+"))throw new ReglaNegocioException("La fotografia debe usar una URL HTTPS o una imagen local del catalogo.");
        request.setNombre(request.getNombre().strip());
        if(request.getNombre().length()<2||request.getNombre().chars().anyMatch(Character::isISOControl))throw new ReglaNegocioException("Ingresa un nombre de producto valido.");
        if(request.getFechaLimiteVenta().isAfter(LocalDateTime.now().plusDays(30)))throw new ReglaNegocioException("El plazo de venta no puede superar los 30 dias.");
        if (request.getFechaLimiteVenta().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha limite de venta debe ser posterior al momento actual.");
        }
        Optional<Producto> existente = productoRepository
                .findByNegocioIdNegocioAndNombreIgnoreCase(request.getIdNegocio(), request.getNombre());
        if (existente.isPresent() && !existente.get().getIdProducto().equals(id)) {
            throw new ReglaNegocioException("Ya existe otro producto con ese nombre en el negocio.");
        }
        Producto producto = buscarPorId(id);
        usuarioActual.verificarNegocio(producto.getNegocio());
        producto.setNegocio(negocioRepository.findById(request.getIdNegocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado.")));
        producto.setCategoria(categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada.")));
        usuarioActual.verificarNegocio(producto.getNegocio());
        aplicarDatos(producto, request);
        Producto actualizado = productoRepository.save(producto);
        return Result.exitoso("Producto actualizado correctamente.", aRespuesta(actualizado));
    }

    @Transactional
    public Result<String> darDeBaja(Integer id) {
        Producto producto = buscarPorId(id);
        usuarioActual.verificarNegocio(producto.getNegocio());
        producto.setActivo(Boolean.FALSE);
        productoRepository.save(producto);
        return Result.exitoso("Producto dado de baja.", "Producto " + id + " desactivado.");
    }

    @Transactional
    public Result<ProductoResponse> cambiarActivo(Integer id, boolean activo) {
        Producto producto=buscarPorId(id);
        usuarioActual.verificarNegocio(producto.getNegocio());
        if(activo && !producto.getFechaLimiteVenta().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("Actualiza la fecha de venta antes de reactivar el producto.");
        }
        producto.setActivo(activo);
        return Result.exitoso(aRespuesta(productoRepository.save(producto)));
    }

    private void aplicarDatos(Producto producto, ProductoRequest request) {
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setPrecioNormal(request.getPrecioNormal());
        producto.setPrecioDescuento(request.getPrecioDescuento());
        producto.setStock(request.getStock());
        producto.setFechaPublicacion(LocalDateTime.now());
        producto.setFechaLimiteVenta(request.getFechaLimiteVenta());
        producto.setEstadoPublicacion(
                request.getEstadoPublicacion() == null ? "BORRADOR" : request.getEstadoPublicacion());
    }

    private void validarPrecios(BigDecimal precioNormal, BigDecimal precioDescuento) {
        if (precioDescuento.compareTo(precioNormal) >= 0) {
            throw new ReglaNegocioException("El precio de descuento debe ser menor al precio normal.");
        }
    }

    private Producto buscarPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }

    private ProductoResponse aRespuesta(Producto producto) {
        ProductoResponse respuesta = new ProductoResponse();
        respuesta.setIdProducto(producto.getIdProducto());
        if (producto.getNegocio() != null) {
            respuesta.setIdNegocio(producto.getNegocio().getIdNegocio());
            respuesta.setNombreNegocio(producto.getNegocio().getNombreComercial());
        }
        if (producto.getCategoria() != null) {
            respuesta.setIdCategoria(producto.getCategoria().getIdCategoria());
            respuesta.setNombreCategoria(producto.getCategoria().getNombre());
        }
        respuesta.setNombre(producto.getNombre());
        respuesta.setDescripcion(producto.getDescripcion());
        respuesta.setImagenUrl(producto.getImagenUrl());
        respuesta.setPrecioNormal(producto.getPrecioNormal());
        respuesta.setPrecioDescuento(producto.getPrecioDescuento());
        respuesta.setDescuentoPorcentaje(calcularDescuento(producto));
        respuesta.setStock(producto.getStock());
        respuesta.setFechaPublicacion(producto.getFechaPublicacion());
        respuesta.setFechaLimiteVenta(producto.getFechaLimiteVenta());
        respuesta.setEstadoPublicacion(producto.getEstadoPublicacion());
        respuesta.setActivo(producto.getActivo());
        return respuesta;
    }

    private BigDecimal calcularDescuento(Producto producto) {
        if (producto.getPrecioNormal() == null
                || producto.getPrecioNormal().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return producto.getPrecioNormal().subtract(producto.getPrecioDescuento())
                .multiply(BigDecimal.valueOf(100))
                .divide(producto.getPrecioNormal(), 2, RoundingMode.HALF_UP);
    }
}
