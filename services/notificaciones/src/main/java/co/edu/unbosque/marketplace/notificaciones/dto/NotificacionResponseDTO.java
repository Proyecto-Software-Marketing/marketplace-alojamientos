package co.edu.unbosque.marketplace.notificaciones.dto;

import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;

import java.time.LocalDateTime;

// Respuesta con la notificación registrada (Notification en el contrato)
public record NotificacionResponseDTO(
        Long id,
        Long usuarioId,
        String tipoEvento,
        String entidadTipo,
        Long entidadId,
        String mensaje,
        boolean leida,
        LocalDateTime creadaEn) {

    public static NotificacionResponseDTO desde(Notificacion notificacion) {
        return new NotificacionResponseDTO(
                notificacion.getId(),
                notificacion.getIdUsuario(),
                notificacion.getTipoEvento().name(),
                notificacion.getEntidadTipo().name(),
                notificacion.getIdEntidad(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion());
    }
}
