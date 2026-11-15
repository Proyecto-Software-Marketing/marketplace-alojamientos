package co.edu.unbosque.marketplace.reservas.client;

import java.util.List;

// Respuesta de POST /internal/availability/check (AvailabilityResult en el contrato)
public record DisponibilidadResultadoDTO(List<Long> disponibles) {
}
