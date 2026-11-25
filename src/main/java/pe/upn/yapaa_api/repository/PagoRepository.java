package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    long countByPedidoIdPedido(Integer idPedido);

    Page<Pago> findByPedidoIdPedido(Integer idPedido, Pageable pageable);

    Page<Pago> findByEstadoPago(String estadoPago, Pageable pageable);
}
