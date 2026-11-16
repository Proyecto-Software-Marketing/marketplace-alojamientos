package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.ServicioAdicional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// HU-10 (CA-129): servicio adicional con su costo. Se usa al recibirlo y al responderlo.
// Decisión 6: nombre de 1 a 60 caracteres y costo entero en pesos, mayor que cero (CA-031)
public record ServicioAdicionalDTO(
        @NotBlank(message = ServicioAdicionalDTO.MENSAJE_NOMBRE)
        @Size(max = ServicioAdicional.LONGITUD_MAXIMA_NOMBRE, message = ServicioAdicionalDTO.MENSAJE_NOMBRE)
        String nombre,

        @NotNull(message = ServicioAdicionalDTO.MENSAJE_COSTO)
        @DecimalMin(value = "1", message = ServicioAdicionalDTO.MENSAJE_COSTO)
        @Digits(integer = 9, fraction = 0, message = ServicioAdicionalDTO.MENSAJE_COSTO)
        BigDecimal costo) {

    public static final String MENSAJE_NOMBRE =
            "Cada servicio adicional debe tener un nombre de máximo 60 caracteres";
    public static final String MENSAJE_COSTO = "El costo de cada servicio adicional debe ser un valor entero en "
            + "pesos, mayor que cero y de máximo 9 dígitos";

    public static ServicioAdicionalDTO desde(ServicioAdicional servicio) {
        return new ServicioAdicionalDTO(servicio.getNombre(), servicio.getCosto());
    }
}
