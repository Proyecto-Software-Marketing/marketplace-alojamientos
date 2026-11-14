package co.edu.unbosque.marketplace.disponibilidad.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Cuerpo de /internal/availability/block y /release (BlockRequest en el contrato)
public record BloqueoRequestDTO(
        @NotNull(message = "La publicación es obligatoria") Long publicacionId,
        @NotNull(message = "La reserva es obligatoria") Long reservaId,
        @NotNull(message = "La fecha de llegada es obligatoria") LocalDate fechaLlegada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate fechaSalida) {
}
