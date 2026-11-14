package co.edu.unbosque.marketplace.disponibilidad.dto;

import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;

import java.time.LocalDate;

// Respuesta con un periodo (Period en el contrato)
public record PeriodoResponseDTO(Long id, Long idPublicacion, LocalDate fechaInicio, LocalDate fechaFin,
        String estado) {

    public static PeriodoResponseDTO desde(PeriodoDisponible periodo) {
        return new PeriodoResponseDTO(periodo.getId(), periodo.getIdPublicacion(),
                periodo.getFechaInicio(), periodo.getFechaFin(), "DISPONIBLE");
    }
}