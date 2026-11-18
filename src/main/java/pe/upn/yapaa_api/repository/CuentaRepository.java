package pe.upn.yapaa_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yapaa_api.model.Cuenta;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    Page<Cuenta> findByEstado(String estado, Pageable pageable);
}
