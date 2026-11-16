package co.edu.unbosque.marketplace.identidad.repository;

import co.edu.unbosque.marketplace.identidad.model.RecuperacionContrasena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, Long> {

    // CA-011 y CA-012 busca la solicitud del token
    Optional<RecuperacionContrasena> findByToken(String token);

    // Solicitudes sin usar de un usuario, para anularlas cuando pide otra
    List<RecuperacionContrasena> findByUsuarioIdAndUsadoFalse(Long idUsuario);
}
