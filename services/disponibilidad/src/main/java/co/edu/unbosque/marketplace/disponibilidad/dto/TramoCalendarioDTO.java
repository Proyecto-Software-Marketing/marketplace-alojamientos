package co.edu.unbosque.marketplace.disponibilidad.dto;

import co.edu.unbosque.marketplace.disponibilidad.model.EstadoCalendario;

import java.time.LocalDate;

// Un tramo del calendario. id es el periodo al que pertenece (Period en el contrato)
public record TramoCalendarioDTO(Long id, LocalDate fechaInicio, LocalDate fechaFin, EstadoCalendario estado) {
}