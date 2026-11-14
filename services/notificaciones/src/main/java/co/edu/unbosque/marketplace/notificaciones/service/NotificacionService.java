package co.edu.unbosque.marketplace.notificaciones.service;

import co.edu.unbosque.marketplace.notificaciones.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEvento;
import co.edu.unbosque.marketplace.notificaciones.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

// HU-30: registra la notificación de un evento para un usuario afectado
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final Clock reloj;

    public NotificacionService(NotificacionRepository notificacionRepository, Clock reloj) {
        this.notificacionRepository = notificacionRepository;
        this.reloj = reloj;
    }

    // CA-083 y CA-084
    @Transactional
    public RegistroNotificacion registrar(Long idUsuario, String tipoEventoTexto, String entidadTipoTexto,
            Long idEntidad, String mensaje) {
        TipoEvento tipoEvento = TipoEvento.desde(tipoEventoTexto)
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "Tipo de evento no válido. Valores permitidos: " + Arrays.toString(TipoEvento.values())));

        // Decisión 2: la entidad se deduce del evento; si la envían, debe coincidir
        if (entidadTipoTexto != null && !entidadTipoTexto.isBlank()
                && !tipoEvento.getEntidadTipo().name().equalsIgnoreCase(entidadTipoTexto.trim())) {
            throw new SolicitudInvalidaException("El evento " + tipoEvento + " corresponde a una entidad de tipo "
                    + tipoEvento.getEntidadTipo());
        }

        // Decisión 3: si el servicio que avisa reintenta, no se duplica la notificación
        Optional<Notificacion> existente = notificacionRepository
                .findByIdUsuarioAndTipoEventoAndIdEntidad(idUsuario, tipoEvento, idEntidad);
        if (existente.isPresent()) {
            return new RegistroNotificacion(existente.get(), false);
        }

        // Decisión 4: si no envían mensaje se usa el del tipo de evento
        String texto = (mensaje == null || mensaje.isBlank()) ? tipoEvento.getMensajePorDefecto() : mensaje.trim();

        Notificacion nueva = new Notificacion(idUsuario, tipoEvento, idEntidad, texto, LocalDateTime.now(reloj));
        return new RegistroNotificacion(notificacionRepository.save(nueva), true);
    }
}
