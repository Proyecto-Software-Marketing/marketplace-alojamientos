package co.edu.unbosque.marketplace.disponibilidad.dto;

import java.util.List;

// Respuesta de /internal/availability/check (AvailabilityResult en el contrato)
public record DisponibilidadResultadoDTO(List<Long> disponibles) {
}
