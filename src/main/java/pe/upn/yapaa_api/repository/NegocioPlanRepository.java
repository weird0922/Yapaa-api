package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.NegocioPlan;

import java.util.Optional;

public interface NegocioPlanRepository extends JpaRepository<NegocioPlan, Integer> {

    Optional<NegocioPlan> findFirstByNegocioIdNegocioAndEstadoOrderByFechaInicioDesc(
            Integer idNegocio, String estado);

    Page<NegocioPlan> findByNegocioIdNegocio(Integer idNegocio, Pageable pageable);

    Page<NegocioPlan> findByEstado(String estado, Pageable pageable);
}
