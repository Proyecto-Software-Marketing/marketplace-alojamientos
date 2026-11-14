package co.edu.unbosque.marketplace.notificaciones.repository;

import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Decisión 3: detectar un reintento del mismo evento para el mismo usuario
    Optional<Notificacion> findByIdUsuarioAndTipoEventoAndIdEntidad(Long idUsuario, TipoEvento tipoEvento,
            Long idEntidad);

    // CA-083: todas las notificaciones que generó un evento sobre una entidad
    List<Notificacion> findByTipoEventoAndIdEntidad(TipoEvento tipoEvento, Long idEntidad);
}
