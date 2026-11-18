package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.pedido.ConfirmarPagoRequest;
import pe.upn.yapaa_api.dto.pedido.DetallePedidoResponse;
import pe.upn.yapaa_api.dto.pedido.GrupoPedidoRequest;
import pe.upn.yapaa_api.dto.pedido.ItemProductoRequest;
import pe.upn.yapaa_api.dto.pedido.PedidoNegocioResponse;
import pe.upn.yapaa_api.dto.pedido.PedidoRequest;
import pe.upn.yapaa_api.dto.pedido.PedidoResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Comision;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.DetallePedido;
import pe.upn.yapaa_api.model.Negocio;
import pe.upn.yapaa_api.model.NegocioPlan;
import pe.upn.yapaa_api.model.Pago;
import pe.upn.yapaa_api.model.Pedido;
import pe.upn.yapaa_api.model.PedidoNegocio;
import pe.upn.yapaa_api.model.Producto;
import pe.upn.yapaa_api.repository.ComisionRepository;
import pe.upn.yapaa_api.repository.NegocioPlanRepository;
import pe.upn.yapaa_api.repository.NegocioRepository;
import pe.upn.yapaa_api.repository.PagoRepository;
import pe.upn.yapaa_api.repository.PedidoRepository;
import pe.upn.yapaa_api.repository.ProductoRepository;
import pe.upn.yapaa_api.security.UsuarioActual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class PedidoService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE", "CONFIRMADO", "EN_PREPARACION", "LISTO",
            "EN_CAMINO", "COMPLETADO", "CANCELADO");

    private static final DateTimeFormatter FORMATO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final NegocioRepository negocioRepository;
    private final UsuarioActual usuarioActual;

    private final Random azar = new Random();

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         NegocioRepository negocioRepository,
                         UsuarioActual usuarioActual) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.negocioRepository = negocioRepository;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public Result<PedidoResponse> crear(PedidoRequest request) {
        if (!usuarioActual.tieneRol("CLIENTE") || usuarioActual.tieneRol("NEGOCIO")) {
            throw new org.springframework.security.access.AccessDeniedException("Solo una cuenta de cliente puede realizar pedidos.");
        }
        Cuenta cuenta = obtenerCuentaAutenticada();

        Pedido pedido = new Pedido();
        pedido.setCuenta(cuenta);
        pedido.setCodigoPedido(generarCodigoUnico());

        BigDecimal subtotalPedido = BigDecimal.ZERO;
        BigDecimal deliveryPedido = BigDecimal.ZERO;

        for (GrupoPedidoRequest grupo : request.getGrupos()) {
            PedidoNegocio subpedido = construirSubpedido(pedido, grupo);
            subtotalPedido = subtotalPedido.add(subpedido.getSubtotalProductos());
            deliveryPedido = deliveryPedido.add(subpedido.getCostoDelivery());
            pedido.getPedidosNegocio().add(subpedido);
        }

        pedido.setSubtotalProductos(subtotalPedido);
        pedido.setCostoDeliveryTotal(deliveryPedido);
        pedido.setTotal(subtotalPedido.add(deliveryPedido));

        Pedido guardado = pedidoRepository.save(pedido);
        return Result.exitoso("Pedido registrado correctamente.", aRespuesta(guardado));
    }

    @Transactional(readOnly = true)
    public Result<Page<PedidoResponse>> listarPorCuenta(Integer idCuenta, Pageable pageable) {
        usuarioActual.verificarCuenta(idCuenta);
        Page<PedidoResponse> pagina = pedidoRepository.findByCuentaIdCuenta(idCuenta, pageable)
                .map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<Page<PedidoResponse>> listarPorEstado(String estadoPedido, Pageable pageable) {
        Page<PedidoResponse> pagina = pedidoRepository.findByEstadoPedido(estadoPedido, pageable)
                .map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<PedidoResponse> obtenerPorId(Integer id) {
        Pedido pedido = buscarPorId(id);
        usuarioActual.verificarPedido(pedido);
        return Result.exitoso(aRespuesta(pedido));
    }

    @Transactional
    public Result<PedidoResponse> cambiarEstado(Integer idPedido, String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new ReglaNegocioException("Estado de pedido invalido: " + estado);
        }
        if (Set.of("COMPLETADO", "CANCELADO").contains(estado)) {
            throw new ReglaNegocioException("Confirma o cancela cada venta desde el panel del negocio para mantener comisiones y stock consistentes.");
        }
        Pedido pedido = buscarPorId(idPedido);
        if (pedido.getPedidosNegocio().stream().anyMatch(v -> Set.of("COMPLETADO", "CANCELADO").contains(v.getEstado()))) {
            throw new ReglaNegocioException("Un pedido con ventas finalizadas no puede reabrirse.");
        }
        usuarioActual.verificarPedido(pedido);
        pedido.setEstadoPedido(estado);
        for (PedidoNegocio subpedido : pedido.getPedidosNegocio()) {
            subpedido.setEstado(estado);
        }
        Pedido actualizado = pedidoRepository.save(pedido);
        return Result.exitoso("Estado del pedido actualizado.", aRespuesta(actualizado));
    }

    private PedidoNegocio construirSubpedido(Pedido pedido, GrupoPedidoRequest grupo) {
        Set<Integer> idsProductos = new HashSet<>();
        for (ItemProductoRequest item : grupo.getItems()) {
            if (!idsProductos.add(item.getIdProducto())) {
                throw new ReglaNegocioException("El producto " + item.getIdProducto()
                        + " esta repetido en el pedido.");
            }
        }

        Negocio negocio = negocioRepository.findById(grupo.getIdNegocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado: " + grupo.getIdNegocio()));

        if (!"APROBADO".equals(negocio.getEstadoAprobacion()) || !Boolean.TRUE.equals(negocio.getActivo())) {
            throw new ReglaNegocioException("El negocio no esta habilitado para recibir pedidos: "
                    + negocio.getNombreComercial());
        }

        boolean delivery = "DELIVERY".equals(grupo.getModalidadEntrega());
        if (delivery && (grupo.getDireccionEntrega() == null || grupo.getDireccionEntrega().isBlank())) {
            throw new ReglaNegocioException("La direccion de entrega es obligatoria para la modalidad DELIVERY.");
        }

        PedidoNegocio subpedido = new PedidoNegocio();
        subpedido.setPedido(pedido);
        subpedido.setNegocio(negocio);
        subpedido.setModalidadEntrega(grupo.getModalidadEntrega());
        subpedido.setDireccionEntrega(delivery ? grupo.getDireccionEntrega() : null);
        subpedido.setCostoDelivery(delivery && grupo.getCostoDelivery() != null
                ? grupo.getCostoDelivery() : BigDecimal.ZERO);
        subpedido.setEstado("PENDIENTE");

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDateTime limite = LocalDateTime.now().plusMinutes(1);

        for (ItemProductoRequest item : grupo.getItems().stream().sorted(java.util.Comparator.comparing(ItemProductoRequest::getIdProducto)).toList()) {
            Producto producto = productoRepository.buscarParaActualizar(item.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto no encontrado: " + item.getIdProducto()));

            if (!producto.getNegocio().getIdNegocio().equals(negocio.getIdNegocio())) {
                throw new ReglaNegocioException("El producto " + producto.getNombre()
                        + " no pertenece al negocio indicado.");
            }
            validarDisponible(producto, item.getCantidad(), limite);
            if (productoRepository.descontarStock(producto.getIdProducto(), item.getCantidad()) != 1) {
                throw new ReglaNegocioException("Stock insuficiente para el producto: " + producto.getNombre());
            }

            DetallePedido detalle = new DetallePedido();
            detalle.setPedidoNegocio(subpedido);
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecioDescuento());
            BigDecimal subtotalItem = producto.getPrecioDescuento()
                    .multiply(BigDecimal.valueOf(item.getCantidad()));
            detalle.setSubtotal(subtotalItem);
            subpedido.getDetalles().add(detalle);

            subtotal = subtotal.add(subtotalItem);
        }

        subpedido.setSubtotalProductos(subtotal);
        subpedido.setTotal(subtotal.add(subpedido.getCostoDelivery()));
        return subpedido;
    }

    private void validarDisponible(Producto producto, Integer cantidad, LocalDateTime limite) {
        if (!"PUBLICADO".equals(producto.getEstadoPublicacion()) || !Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("El producto no esta publicado: " + producto.getNombre());
        }
        if (producto.getFechaLimiteVenta().isBefore(limite)) {
            throw new ReglaNegocioException("El plazo de venta del producto ha finalizado: " + producto.getNombre());
        }
        if (producto.getStock() < cantidad) {
            throw new ReglaNegocioException("Stock insuficiente para el producto: " + producto.getNombre());
        }
        Negocio negocio = producto.getNegocio();
        if (!"APROBADO".equals(negocio.getEstadoAprobacion()) || !Boolean.TRUE.equals(negocio.getActivo())) {
            throw new ReglaNegocioException("El negocio del producto no esta habilitado: " + producto.getNombre());
        }
    }

    private Cuenta obtenerCuentaAutenticada() {
        Cuenta cuenta = usuarioActual.obtenerCuenta();
        if (!"ACTIVO".equals(cuenta.getEstado())) {
            throw new ReglaNegocioException("La cuenta no esta activa.");
        }
        return cuenta;
    }

    private Pedido buscarPorId(Integer id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = "YAP-" + LocalDateTime.now().format(FORMATO_CODIGO)
                    + String.format("%03d", azar.nextInt(1000));
        } while (pedidoRepository.existsByCodigoPedido(codigo));
        return codigo;
    }

    private PedidoResponse aRespuesta(Pedido pedido) {
        PedidoResponse respuesta = new PedidoResponse();
        respuesta.setIdPedido(pedido.getIdPedido());
        respuesta.setCodigoPedido(pedido.getCodigoPedido());
        respuesta.setFechaPedido(pedido.getFechaPedido());
        if (pedido.getCuenta() != null) {
            respuesta.setIdCuenta(pedido.getCuenta().getIdCuenta());
            respuesta.setCorreoCliente(pedido.getCuenta().getCorreo());
        }
        respuesta.setSubtotalProductos(pedido.getSubtotalProductos());
        respuesta.setCostoDeliveryTotal(pedido.getCostoDeliveryTotal());
        respuesta.setTotal(pedido.getTotal());
        respuesta.setEstadoPedido(pedido.getEstadoPedido());
        respuesta.setEstadoPago(pedido.getEstadoPago());

        for (PedidoNegocio subpedido : pedido.getPedidosNegocio()) {
            respuesta.getPedidosNegocio().add(aRespuestaSubpedido(subpedido, pedido.getCodigoPedido()));
        }
        return respuesta;
    }

    private PedidoNegocioResponse aRespuestaSubpedido(PedidoNegocio subpedido, String codigoPedido) {
        PedidoNegocioResponse respuesta = new PedidoNegocioResponse();
        respuesta.setIdPedidoNegocio(subpedido.getIdPedidoNegocio());
        if (subpedido.getNegocio() != null) {
            respuesta.setIdNegocio(subpedido.getNegocio().getIdNegocio());
            respuesta.setNombreNegocio(subpedido.getNegocio().getNombreComercial());
            respuesta.setTelefonoContacto(subpedido.getNegocio().getTelefonoContacto());
        }
        respuesta.setModalidadEntrega(subpedido.getModalidadEntrega());
        respuesta.setDireccionEntrega(subpedido.getDireccionEntrega());
        respuesta.setCostoDelivery(subpedido.getCostoDelivery());
        respuesta.setSubtotalProductos(subpedido.getSubtotalProductos());
        respuesta.setTotal(subpedido.getTotal());
        respuesta.setEstado(subpedido.getEstado());
        respuesta.setFechaRecojoProgramada(subpedido.getFechaRecojoProgramada());

        String mensaje = construirMensaje(subpedido, codigoPedido);
        respuesta.setMensaje(mensaje);
        respuesta.setEnlaceWhatsApp(construirEnlaceWhatsApp(respuesta.getTelefonoContacto(), mensaje));

        List<DetallePedidoResponse> detalles = new ArrayList<>();
        respuesta.setDetalles(detalles);
        for (DetallePedido detalle : subpedido.getDetalles()) {
            DetallePedidoResponse item = new DetallePedidoResponse();
            item.setIdDetalle(detalle.getIdDetalle());
            if (detalle.getProducto() != null) {
                item.setIdProducto(detalle.getProducto().getIdProducto());
            }
            item.setNombreProducto(detalle.getNombreProducto());
            item.setCantidad(detalle.getCantidad());
            item.setPrecioUnitario(detalle.getPrecioUnitario());
            item.setSubtotal(detalle.getSubtotal());
            detalles.add(item);
        }
        return respuesta;
    }

    private String construirMensaje(PedidoNegocio subpedido, String codigoPedido) {
        String nombre = subpedido.getNegocio() != null
                ? subpedido.getNegocio().getNombreComercial() : "negocio";
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Hola ").append(nombre).append(", deseo realizar este pedido en YAPAA:");
        mensaje.append("\nPedido: ").append(codigoPedido);
        for (DetallePedido detalle : subpedido.getDetalles()) {
            mensaje.append("\n- ").append(detalle.getCantidad()).append("x ")
                    .append(detalle.getNombreProducto())
                    .append(" (S/ ").append(detalle.getSubtotal()).append(")");
        }
        mensaje.append("\nSubtotal: S/ ").append(subpedido.getSubtotalProductos());
        mensaje.append("\nDelivery: S/ ").append(subpedido.getCostoDelivery());
        mensaje.append("\nTotal: S/ ").append(subpedido.getTotal());
        mensaje.append("\nModalidad: ").append(subpedido.getModalidadEntrega());
        if (subpedido.getDireccionEntrega() != null && !subpedido.getDireccionEntrega().isBlank()) {
            mensaje.append("\nDireccion: ").append(subpedido.getDireccionEntrega());
        }
        return mensaje.toString();
    }

    private String construirEnlaceWhatsApp(String telefono, String mensaje) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String soloDigitos = telefono.replaceAll("[^0-9]", "");
        if (soloDigitos.isEmpty()) {
            return null;
        }
        if (soloDigitos.length() == 9) {
            soloDigitos = "51" + soloDigitos;
        }
        String texto = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        return "https://wa.me/" + soloDigitos + "?text=" + texto;
    }
}
