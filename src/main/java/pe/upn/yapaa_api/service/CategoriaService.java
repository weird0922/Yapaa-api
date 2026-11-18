package pe.upn.yapaa_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upn.yapaa_api.common.Result;
import pe.upn.yapaa_api.dto.categoria.CategoriaRequest;
import pe.upn.yapaa_api.dto.categoria.CategoriaResponse;
import pe.upn.yapaa_api.exception.RecursoNoEncontradoException;
import pe.upn.yapaa_api.exception.ReglaNegocioException;
import pe.upn.yapaa_api.model.Categoria;
import pe.upn.yapaa_api.repository.CategoriaRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Result<Page<CategoriaResponse>> listar(Boolean activo, Pageable pageable) {
        Page<Categoria> pagina = activo == null
                ? categoriaRepository.findAll(pageable)
                : categoriaRepository.findByActivo(activo, pageable);
        return Result.exitoso(pagina.map(this::aRespuesta));
    }

    @Transactional(readOnly = true)
    public Result<CategoriaResponse> obtenerPorId(Integer id) {
        Categoria categoria = buscarPorId(id);
        return Result.exitoso(aRespuesta(categoria));
    }

    @Transactional
    public Result<CategoriaResponse> crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ReglaNegocioException("Ya existe una categoria con el nombre indicado.");
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setActivo(request.getActivo() == null ? Boolean.TRUE : request.getActivo());
        Categoria guardada = categoriaRepository.save(categoria);
        return Result.exitoso("Categoria creada correctamente.", aRespuesta(guardada));
    }

    @Transactional
    public Result<CategoriaResponse> actualizar(Integer id, CategoriaRequest request) {
        Categoria categoria = buscarPorId(id);
        if (!categoria.getNombre().equalsIgnoreCase(request.getNombre())
                && categoriaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ReglaNegocioException("Ya existe otra categoria con el nombre indicado.");
        }
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }
        Categoria actualizada = categoriaRepository.save(categoria);
        return Result.exitoso("Categoria actualizada correctamente.", aRespuesta(actualizada));
    }

    @Transactional
    public Result<String> eliminar(Integer id) {
        Categoria categoria = buscarPorId(id);
        categoria.setActivo(Boolean.FALSE);
        categoriaRepository.save(categoria);
        return Result.exitoso("Categoria desactivada correctamente.", "Categoria " + id + " desactivada.");
    }

    private Categoria buscarPorId(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada: " + id));
    }

    private CategoriaResponse aRespuesta(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getIdCategoria(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActivo());
    }
}
