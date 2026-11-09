package co.edu.unbosque.marketplace.identidad.repository;

import co.edu.unbosque.marketplace.identidad.model.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    // Busca una sesión que siga activa con ese token
    Optional<Sesion> findByTokenAndActivaTrue(String token);
}