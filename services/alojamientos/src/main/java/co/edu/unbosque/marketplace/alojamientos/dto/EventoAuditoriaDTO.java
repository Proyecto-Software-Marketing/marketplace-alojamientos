
package co.edu.unbosque.marketplace.alojamientos.dto;

// Datos que Alojamientos envia al microservicio de Auditoria.
public record EventoAuditoriaDTO(
        Long usuarioId,
        String entidad,
        Long entidadId,
        String accion,
        String resultado,
        String detalle) {
}
