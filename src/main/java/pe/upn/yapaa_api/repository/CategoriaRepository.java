package pe.upn.yapaa_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Categoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Categoria> findByActivo(Boolean activo, Pageable pageable);

    List<Categoria> findByActivoTrue();
}
