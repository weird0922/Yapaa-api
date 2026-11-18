package pe.upn.yapaa_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.DetallePedido;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {

    List<DetallePedido> findByPedidoNegocioIdPedidoNegocio(Integer idPedidoNegocio);

    void deleteByPedidoNegocioIdPedidoNegocio(Integer idPedidoNegocio);
}
