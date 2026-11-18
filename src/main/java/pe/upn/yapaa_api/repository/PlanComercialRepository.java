package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.PlanComercial;

import java.util.Optional;

public interface PlanComercialRepository extends JpaRepository<PlanComercial, Integer> {

    Optional<PlanComercial> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    Page<PlanComercial> findByActivo(Boolean activo, Pageable pageable);
}
