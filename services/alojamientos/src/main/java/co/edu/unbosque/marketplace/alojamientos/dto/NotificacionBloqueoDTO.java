package co.edu.unbosque.marketplace.alojamientos.dto;

// HU-34 (CA-096): datos enviados a Notificaciones.
public record NotificacionBloqueoDTO(
        Long usuarioId,
        String tipoEvento,
        String entidadTipo,
        Long entidadId,
        String mensaje) {
}