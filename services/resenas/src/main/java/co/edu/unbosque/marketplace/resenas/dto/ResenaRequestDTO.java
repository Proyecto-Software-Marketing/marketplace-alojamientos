package co.edu.unbosque.marketplace.resenas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Cuerpo de POST /bookings/{id}/review (ReviewInput en el contrato).
// Decisión 1: la calificación se recibe como BigDecimal para rechazar decimales como 4.5
// (si fuera Integer, el JSON 4.5 se recortaría a 4 sin avisar)
public record ResenaRequestDTO(
        @NotNull(message = ResenaRequestDTO.MENSAJE_CALIFICACION)
        @DecimalMin(value = "1", message = ResenaRequestDTO.MENSAJE_CALIFICACION)
        @DecimalMax(value = "5", message = ResenaRequestDTO.MENSAJE_CALIFICACION)
        @Digits(integer = 1, fraction = 0, message = ResenaRequestDTO.MENSAJE_CALIFICACION)
        BigDecimal calificacion,

        @NotBlank(message = "El contenido de la reseña es obligatorio")
        @Size(max = 1000, message = "El contenido de la reseña no puede superar 1000 caracteres")
        String contenido) {

    public static final String MENSAJE_CALIFICACION = "La calificación debe ser un número entero entre 1 y 5";

    // Ya validada: es un entero entre 1 y 5
    public Integer calificacionEntera() {
        return calificacion.intValueExact();
    }
}
