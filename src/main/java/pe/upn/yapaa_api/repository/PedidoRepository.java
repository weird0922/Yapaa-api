package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Pedido;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.idPedido=:id")
    java.util.Optional<Pedido> bloquear(@org.springframework.data.repository.query.Param("id") Integer id);


    Optional<Pedido> findByCodigoPedido(String codigoPedido);

    boolean existsByCodigoPedido(String codigoPedido);

    Page<Pedido> findByCuentaIdCuenta(Integer idCuenta, Pageable pageable);

    Page<Pedido> findByEstadoPedido(String estadoPedido, Pageable pageable);

    Page<Pedido> findByEstadoPago(String estadoPago, Pageable pageable);
}
