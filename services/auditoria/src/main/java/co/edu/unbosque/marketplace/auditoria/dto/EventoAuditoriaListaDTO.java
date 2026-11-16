package co.edu.unbosque.marketplace.auditoria.dto;

import java.util.List;

// Respuesta de la consulta de auditoria: los eventos que el administrador pidio
public record EventoAuditoriaListaDTO(List<EventoAuditoriaResponseDTO> items) {

	public EventoAuditoriaListaDTO {
		items = items == null ? List.of() : List.copyOf(items);
	}

}
