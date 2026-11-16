package co.edu.unbosque.marketplace.reportes.dto;

// Evento que Reportes envía a Auditoría (HU-41, CA-113)
public record EventoAuditoriaDTO(
        Long usuarioId,
        String entidad,
        Long entidadId,
        String accion,
        String resultado,
        String detalle) {
}
