package co.edu.unbosque.marketplace.resenas.dto;

// Cuerpo de POST /internal/audit-events del servicio Auditoría (AuditEvent en su contrato)
public record EventoAuditoriaDTO(
        Long usuarioId,
        String entidad,
        Long entidadId,
        String accion,
        String resultado,
        String detalle) {
}
