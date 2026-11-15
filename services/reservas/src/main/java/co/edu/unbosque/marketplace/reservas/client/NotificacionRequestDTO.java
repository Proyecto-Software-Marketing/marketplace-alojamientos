package co.edu.unbosque.marketplace.reservas.client;

// Cuerpo de POST /internal/notifications (NotificationCreate en el contrato)
public record NotificacionRequestDTO(
        Long usuarioId,
        String tipoEvento,
        String entidadTipo,
        Long entidadId,
        String mensaje) {
}
