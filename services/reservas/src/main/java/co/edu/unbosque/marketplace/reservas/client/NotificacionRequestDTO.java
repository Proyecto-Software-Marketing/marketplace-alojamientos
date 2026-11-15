package co.edu.unbosque.marketplace.reservas.client;

public record NotificacionRequestDTO(
        Long usuarioId,
        String tipoEvento,
        String entidadTipo,
        Long entidadId,
        String mensaje) {
}
