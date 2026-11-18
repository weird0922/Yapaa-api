package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.comision.ComisionResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Comision;
import pe.upn.yapaa_api.repository.ComisionRepository;

import java.time.LocalDateTime;

@Service
public class ComisionService {

    private final ComisionRepository comisionRepository;

    public ComisionService(ComisionRepository comisionRepository) {
        this.comisionRepository = comisionRepository;
    }

    @Transactional(readOnly = true)
    public Result<Page<ComisionResponse>> listar(String estado, Pageable pageable) {
        Page<ComisionResponse> pagina = (estado == null || estado.isBlank())
                ? comisionRepository.findAll(pageable).map(this::aRespuesta)
                : comisionRepository.findByEstado(estado.toUpperCase(), pageable).map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<Page<ComisionResponse>> listarPorNegocio(Integer idNegocio, Pageable pageable) {
        Page<ComisionResponse> pagina = comisionRepository
                .findByPedidoNegocioNegocioIdNegocio(idNegocio, pageable).map(this::aRespuesta);
        return Result.exitoso(pagina);
    }

    @Transactional(readOnly = true)
    public Result<ComisionResponse> obtenerPorId(Integer id) {
        return Result.exitoso(aRespuesta(buscarPorId(id)));
    }

    @Transactional
    public Result<ComisionResponse> liquidar(Integer id) {
        Comision comision = buscarPorId(id);
        if (!"PENDIENTE".equals(comision.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden liquidar comisiones pendientes.");
        }
        comision.setEstado("LIQUIDADA");
        comision.setFechaLiquidacion(LocalDateTime.now());
        Comision actualizada = comisionRepository.save(comision);
        return Result.exitoso("Comision liquidada correctamente.", aRespuesta(actualizada));
    }

    @Transactional
    public Result<ComisionResponse> anular(Integer id) {
        Comision comision = buscarPorId(id);
        if ("LIQUIDADA".equals(comision.getEstado())) {
            throw new ReglaNegocioException("Una comision liquidada no puede anularse.");
        }
        comision.setEstado("ANULADA");
        Comision actualizada = comisionRepository.save(comision);
        return Result.exitoso("Comision anulada correctamente.", aRespuesta(actualizada));
    }

    private Comision buscarPorId(Integer id) {
        return comisionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comision no encontrada: " + id));
    }

    private ComisionResponse aRespuesta(Comision comision) {
        ComisionResponse respuesta = new ComisionResponse();
        respuesta.setIdComision(comision.getIdComision());
        if (comision.getPedidoNegocio() != null) {
            respuesta.setIdPedidoNegocio(comision.getPedidoNegocio().getIdPedidoNegocio());
            if (comision.getPedidoNegocio().getNegocio() != null) {
                respuesta.setIdNegocio(comision.getPedidoNegocio().getNegocio().getIdNegocio());
                respuesta.setNombreNegocio(comision.getPedidoNegocio().getNegocio().getNombreComercial());
            }
        }
        respuesta.setBaseCalculo(comision.getBaseCalculo());
        respuesta.setPorcentajeAplicado(comision.getPorcentajeAplicado());
        respuesta.setMontoComision(comision.getMontoComision());
        respuesta.setEstado(comision.getEstado());
        respuesta.setFechaLiquidacion(comision.getFechaLiquidacion());
        respuesta.setFechaRegistro(comision.getFechaRegistro());
        return respuesta;
    }
}
