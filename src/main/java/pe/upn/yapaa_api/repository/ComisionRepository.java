package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Comision;

import java.util.Optional;

public interface ComisionRepository extends JpaRepository<Comision, Integer> {

    Optional<Comision> findByPedidoNegocioIdPedidoNegocio(Integer idPedidoNegocio);

    Page<Comision> findByEstado(String estado, Pageable pageable);

    Page<Comision> findByPedidoNegocioNegocioIdNegocio(Integer idNegocio, Pageable pageable);
}
