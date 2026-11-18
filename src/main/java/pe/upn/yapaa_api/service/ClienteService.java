package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pe.upn.yapaa_api.security.UsuarioActual;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.cliente.ClienteRequest;
import pe.upn.yapaa_api.dto.cliente.ClienteResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.model.Cliente;
import pe.upn.yapaa_api.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioActual usuarioActual;

    public ClienteService(ClienteRepository clienteRepository, UsuarioActual usuarioActual) {
        this.clienteRepository = clienteRepository;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public Result<Page<ClienteResponse>> listar(String filtro, Pageable pageable) {
        Page<Cliente> pagina = (filtro == null || filtro.isBlank())
                ? clienteRepository.findAll(pageable)
                : clienteRepository.findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(
                        filtro, filtro, pageable);
        return Result.exitoso(pagina.map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public Result<ClienteResponse> obtenerPorId(Integer idCuenta) {
        usuarioActual.verificarCuenta(idCuenta);
        return Result.exitoso(aRespuesta(buscarPorId(idCuenta)));
    }

    @Transactional
    public Result<ClienteResponse> actualizar(Integer idCuenta, ClienteRequest request) {
        usuarioActual.verificarCuenta(idCuenta);
        Cliente cliente = buscarPorId(idCuenta);
        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setTelefono(request.getTelefono());
        Cliente actualizado = clienteRepository.save(cliente);
        return Result.exitoso("Datos del cliente actualizados.", aRespuesta(actualizado));
    }

    private Cliente buscarPorId(Integer idCuenta) {
        return clienteRepository.findById(idCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + idCuenta));
    }

    private ClienteResponse aRespuesta(Cliente cliente) {
        ClienteResponse respuesta = new ClienteResponse();
        respuesta.setIdCuenta(cliente.getIdCuenta());
        if (cliente.getCuenta() != null) {
            respuesta.setCorreo(cliente.getCuenta().getCorreo());
            respuesta.setEstadoCuenta(cliente.getCuenta().getEstado());
        }
        respuesta.setNombres(cliente.getNombres());
        respuesta.setApellidos(cliente.getApellidos());
        respuesta.setTelefono(cliente.getTelefono());
        return respuesta;
    }
}
