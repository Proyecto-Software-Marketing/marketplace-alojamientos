package co.edu.unbosque.marketplace.alojamientos.dto;

// HU-13: datos enviados al microservicio de Auditoria.
public record EventoAuditoriaDTO(
        Long usuarioId,
        String entidad,
        Long entidadId,
        String accion,
        String resultado,
        String detalle) {
}
