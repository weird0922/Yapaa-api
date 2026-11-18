package pe.upn.yapaa_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
