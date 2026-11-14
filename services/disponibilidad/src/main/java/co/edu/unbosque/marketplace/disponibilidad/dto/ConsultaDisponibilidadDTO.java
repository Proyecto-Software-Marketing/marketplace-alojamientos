package co.edu.unbosque.marketplace.disponibilidad.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

// Cuerpo de /internal/availability/check (AvailabilityCheck en el contrato)
public record ConsultaDisponibilidadDTO(
        @NotNull(message = "Las publicaciones son obligatorias") List<Long> publicacionIds,
        @NotNull(message = "La fecha de llegada es obligatoria") LocalDate fechaLlegada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate fechaSalida) {
}
