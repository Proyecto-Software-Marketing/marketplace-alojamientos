package co.edu.unbosque.marketplace.identidad.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import co.edu.unbosque.marketplace.identidad.model.Sesion;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    // Busca una sesión que siga activa con ese token
    Optional<Sesion> findByTokenAndActivaTrue(String token);
    //Busca las sesiones abiertas de un usuario para cerrarlas al desactivar la cuenta
    List<Sesion> findByUsuarioIdAndActivaTrue(Long idUsuario);
}