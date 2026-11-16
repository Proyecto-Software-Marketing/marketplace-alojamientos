package co.edu.unbosque.marketplace.auditoria.dto;

import java.time.LocalDateTime;

import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;

public record EventoAuditoriaResponseDTO(
		Long id,
		Long usuarioId,
		String entidad,
		Long entidadId,
		String accion,
		String resultado,
		String detalle,
		LocalDateTime fecha) {

	public static EventoAuditoriaResponseDTO desde(EventoAuditoria evento) {
		return new EventoAuditoriaResponseDTO(evento.getId(), evento.getUsuarioId(), evento.getEntidad(),
				evento.getEntidadId(), evento.getAccion(), evento.getResultado(), evento.getDetalle(),
				evento.getFecha());
	}

}
