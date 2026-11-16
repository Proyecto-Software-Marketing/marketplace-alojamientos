package co.edu.unbosque.marketplace.auditoria.dto;

import java.time.LocalDateTime;

// Filtros de la consulta de auditoria (HU-41, CA-114)
public record FiltroAuditoriaDTO(
		Long usuario,
		String entidad,
		String accion,
		LocalDateTime desde,
		LocalDateTime hasta) {
}
