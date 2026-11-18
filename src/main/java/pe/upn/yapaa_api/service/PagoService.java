package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.pago.PagoResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Pago;
import pe.upn.yapaa_api.repository.PagoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoService {

    private static final List<String> ESTADOS_VALIDOS =
            List.of("PENDIENTE", "CONFIRMADO", "RECHAZADO", "ANULADO");

    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    @Transactional(readOnly = true)
    public Result<Page<PagoResponse>> listar(String estado, Pageable pageable) {
        Page<PagoResponse> pagina = (estado == null || estado.isBlank())
                ? pagoRepository.findAll(pageable).map(this::aRespuesta)
                : pagoRepository.findByEstadoPago(estado.toUpperCase(), pageable).map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<Page<PagoResponse>> listarPorPedido(Integer idPedido, Pageable pageable) {
        Page<PagoResponse> pagina = pagoRepository.findByPedidoIdPedido(idPedido, pageable)
                .map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<PagoResponse> obtenerPorId(Integer id) {
        return Result.exitoso(aRespuesta(buscarPorId(id)));
    }

    @Transactional
    public Result<PagoResponse> cambiarEstado(Integer id, String estado) {
        String nuevoEstado = estado == null ? "" : estado.toUpperCase();
        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new ReglaNegocioException("Estado de pago invalido: " + estado);
        }
        Pago pago = buscarPorId(id);
        if ("CONFIRMADO".equals(pago.getEstadoPago())) {
            throw new ReglaNegocioException("Un pago confirmado no puede cambiar de estado.");
        }
        pago.setEstadoPago(nuevoEstado);
        if ("CONFIRMADO".equals(nuevoEstado) && pago.getFechaPago() == null) {
            pago.setFechaPago(LocalDateTime.now());
        }
        Pago actualizado = pagoRepository.save(pago);
        return Result.exitoso("Estado de pago actualizado.", aRespuesta(actualizado));
    }

    private Pago buscarPorId(Integer id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + id));
    }

    private PagoResponse aRespuesta(Pago pago) {
        PagoResponse respuesta = new PagoResponse();
        respuesta.setIdPago(pago.getIdPago());
        if (pago.getPedido() != null) {
            respuesta.setIdPedido(pago.getPedido().getIdPedido());
            respuesta.setCodigoPedido(pago.getPedido().getCodigoPedido());
        }
        if (pago.getCuentaRegistro() != null) {
            respuesta.setIdCuentaRegistro(pago.getCuentaRegistro().getIdCuenta());
        }
        respuesta.setMetodoPago(pago.getMetodoPago());
        respuesta.setMonto(pago.getMonto());
        respuesta.setEstadoPago(pago.getEstadoPago());
        respuesta.setReferencia(pago.getReferencia());
        respuesta.setFechaPago(pago.getFechaPago());
        respuesta.setFechaRegistro(pago.getFechaRegistro());
        return respuesta;
    }
}
