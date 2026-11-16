package co.edu.unbosque.marketplace.reservas.dto;

public record EventoAuditoriaDTO(
        Long usuarioId,
        String entidad,
        Long entidadId,
        String accion,
        String resultado,
        String detalle) {
}
