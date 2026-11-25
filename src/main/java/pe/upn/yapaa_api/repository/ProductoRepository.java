package pe.upn.yapaa_api.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.upn.yapaa_api.model.Producto;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Page<Producto> findByNegocioIdNegocio(Integer idNegocio, Pageable pageable);

    Page<Producto> findByNegocioIdNegocioAndEstadoPublicacion(
            Integer idNegocio, String estadoPublicacion, Pageable pageable);

    Page<Producto> findByActivo(Boolean activo, Pageable pageable);

    boolean existsByNegocioIdNegocioAndNombreIgnoreCase(Integer idNegocio, String nombre);

    java.util.Optional<Producto> findByNegocioIdNegocioAndNombreIgnoreCase(Integer idNegocio, String nombre);

    @Query("SELECT p FROM Producto p "
            + "WHERE p.activo = true AND p.estadoPublicacion = 'PUBLICADO' AND p.stock > 0 AND p.fechaLimiteVenta > CURRENT_TIMESTAMP "
            + "AND p.negocio.estadoAprobacion = 'APROBADO' AND p.negocio.activo = true")
    Page<Producto> buscarDisponibles(Pageable pageable);

    @Query("SELECT p FROM Producto p "
            + "WHERE p.categoria.idCategoria = :idCategoria AND p.activo = true AND p.estadoPublicacion = 'PUBLICADO' AND p.stock > 0 AND p.fechaLimiteVenta > CURRENT_TIMESTAMP "
            + "AND p.negocio.estadoAprobacion = 'APROBADO' AND p.negocio.activo = true")
    Page<Producto> buscarDisponiblesPorCategoria(
            @Param("idCategoria") Integer idCategoria, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.negocio.idNegocio = :idNegocio "
            + "AND p.activo = true AND p.estadoPublicacion = 'PUBLICADO' AND p.stock > 0 AND p.fechaLimiteVenta > CURRENT_TIMESTAMP "
            + "AND p.negocio.estadoAprobacion = 'APROBADO' AND p.negocio.activo = true")
    Page<Producto> buscarPublicadosPorNegocio(@Param("idNegocio") Integer idNegocio, Pageable pageable);

    Page<Producto> findByNegocioCuentaIdCuenta(Integer idCuenta, Pageable pageable);

    @Query("SELECT p FROM Producto p JOIN FETCH p.negocio n WHERE p.idProducto IN :ids AND n.nombreComercial IN :nombres")
    java.util.List<Producto> buscarSimulados(@Param("ids") java.util.List<Integer> ids, @Param("nombres") java.util.List<String> nombres);

    @Query("SELECT p FROM Producto p JOIN FETCH p.negocio n WHERE n.ruc LIKE '20999%'")
    java.util.List<Producto> buscarEscenarioDemo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :idProducto")
    Optional<Producto> buscarParaActualizar(@Param("idProducto") Integer idProducto);

    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad "
            + "WHERE p.idProducto = :idProducto AND p.stock >= :cantidad")
    int descontarStock(@Param("idProducto") Integer idProducto, @Param("cantidad") Integer cantidad);
}
