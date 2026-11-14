package co.edu.unbosque.marketplace.disponibilidad.dto;

import java.util.List;

// Respuesta de GET /listings/{id}/calendar (Calendar en el contrato)
public record CalendarioResponseDTO(Long idPublicacion, List<TramoCalendarioDTO> periodos) {
}