package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Negocio;

import java.util.List;
import java.util.Optional;

public interface NegocioRepository extends JpaRepository<Negocio, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select n from Negocio n where n.idNegocio=:id")
    Optional<Negocio> bloquear(@org.springframework.data.repository.query.Param("id") Integer id);

    Page<Negocio> findByEstadoAprobacionAndActivoTrue(String estadoAprobacion, Pageable pageable);

    Optional<Negocio> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    Page<Negocio> findByEstadoAprobacion(String estadoAprobacion, Pageable pageable);

    Page<Negocio> findByCuentaIdCuenta(Integer idCuenta, Pageable pageable);

    List<Negocio> findByEstadoAprobacionAndActivoTrue(String estadoAprobacion);
}
