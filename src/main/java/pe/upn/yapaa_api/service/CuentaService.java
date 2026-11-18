package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.cuenta.CuentaResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Cuenta;
import pe.upn.yapaa_api.model.Rol;
import pe.upn.yapaa_api.repository.CuentaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public Result<Page<CuentaResponse>> listar(String estado, Pageable pageable) {
        Page<Cuenta> pagina = (estado == null || estado.isBlank())
                ? cuentaRepository.findAll(pageable)
                : cuentaRepository.findByEstado(estado.toUpperCase(), pageable);
        return Result.exitoso(pagina.map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public Result<CuentaResponse> obtenerPorId(Integer id) {
        return Result.exitoso(aRespuesta(buscarPorId(id)));
    }

    @Transactional
    public Result<CuentaResponse> cambiarEstado(Integer id, String estado) {
        String nuevoEstado = estado == null ? "" : estado.toUpperCase();
        if (!nuevoEstado.equals("ACTIVO") && !nuevoEstado.equals("INACTIVO")) {
            throw new ReglaNegocioException("Estado de cuenta invalido: " + estado);
        }
        Cuenta cuenta = buscarPorId(id);
        cuenta.setEstado(nuevoEstado);
        Cuenta actualizada = cuentaRepository.save(cuenta);
        return Result.exitoso("Estado de cuenta actualizado.", aRespuesta(actualizada));
    }

    private Cuenta buscarPorId(Integer id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada: " + id));
    }

    private CuentaResponse aRespuesta(Cuenta cuenta) {
        CuentaResponse respuesta = new CuentaResponse();
        respuesta.setIdCuenta(cuenta.getIdCuenta());
        respuesta.setCorreo(cuenta.getCorreo());
        respuesta.setEstado(cuenta.getEstado());
        respuesta.setFechaRegistro(cuenta.getFechaRegistro());
        respuesta.setFechaActualizacion(cuenta.getFechaActualizacion());
        List<String> roles = cuenta.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toList());
        respuesta.setRoles(roles);
        return respuesta;
    }
}
