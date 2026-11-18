package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.PedidoNegocio;

public interface PedidoNegocioRepository extends JpaRepository<PedidoNegocio, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PedidoNegocio p where p.idPedidoNegocio=:id")
    java.util.Optional<PedidoNegocio> bloquear(@org.springframework.data.repository.query.Param("id") Integer id);
    java.util.List<PedidoNegocio> findByNegocioIdNegocioAndEstado(Integer idNegocio,String estado);


    Page<PedidoNegocio> findByPedidoIdPedido(Integer idPedido, Pageable pageable);

    Page<PedidoNegocio> findByNegocioIdNegocio(Integer idNegocio, Pageable pageable);

    Page<PedidoNegocio> findByNegocioIdNegocioAndEstado(Integer idNegocio, String estado, Pageable pageable);
}
