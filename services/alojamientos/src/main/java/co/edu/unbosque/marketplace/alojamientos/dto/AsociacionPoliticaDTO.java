package co.edu.unbosque.marketplace.alojamientos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsociacionPoliticaDTO(
        @NotNull(message = "Debe seleccionar una politica")
        @Positive(message = "La politica seleccionada no es valida")
        Long politicaId) {
}