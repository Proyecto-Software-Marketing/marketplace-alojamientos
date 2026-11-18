package co.edu.unbosque.marketplace.alojamientos.dto;

// HU-34 (CA-096): evento enviado a Auditoria.
public record EventoAuditoriaBloqueoDTO(
        Long usuarioId,
        String entidad,
        Long entidadId,
        String accion,
        String resultado,
        String detalle) {
}