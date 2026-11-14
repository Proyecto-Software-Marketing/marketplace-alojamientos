package co.edu.unbosque.marketplace.disponibilidad.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Cuerpo de POST y PUT /listings/{id}/availability (PeriodInput en el contrato)
public record PeriodoRequestDTO(
        @NotNull(message = "La fecha de llegada es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate fechaFin) {
}