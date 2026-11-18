package co.edu.unbosque.marketplace.alojamientos.dto;

// HU-13: datos enviados al microservicio de Notificaciones.
public record NotificacionRequestDTO(
        Long usuarioId,
        String tipoEvento,
        String entidadTipo,
        Long entidadId,
        String mensaje) {
}
