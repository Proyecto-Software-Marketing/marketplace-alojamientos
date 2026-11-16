package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Cuerpo de POST /listings (ListingInput en el contrato).
// Decisión 2: usa los nombres del contrato (tipoAlojamiento, precioPorNoche).
// La capacidad se recibe como BigDecimal para rechazar decimales como 2.5
// (si fuera Integer, el JSON 2.5 se recortaría a 2 sin avisar)
public record PublicacionRequestDTO(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = Publicacion.LONGITUD_MAXIMA_TITULO, message = "El título no puede superar 120 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = Publicacion.LONGITUD_MAXIMA_DESCRIPCION,
                message = "La descripción no puede superar 2000 caracteres")
        String descripcion,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = Publicacion.LONGITUD_MAXIMA_CIUDAD, message = "La ciudad no puede superar 80 caracteres")
        String ciudad,

        @NotBlank(message = "La ubicación es obligatoria")
        @Size(max = Publicacion.LONGITUD_MAXIMA_UBICACION, message = "La ubicación no puede superar 200 caracteres")
        String ubicacionTextual,

        // Decisión 7: se recibe como texto para responder con la lista de valores válidos
        @NotNull(message = PublicacionRequestDTO.MENSAJE_TIPO)
        @Pattern(regexp = "APARTAMENTO|CASA|HABITACION|CABANA|OTRO", message = PublicacionRequestDTO.MENSAJE_TIPO)
        String tipoAlojamiento,

        // CA-021 y decisión 8
        @NotNull(message = PublicacionRequestDTO.MENSAJE_CAPACIDAD)
        @DecimalMin(value = "1", message = PublicacionRequestDTO.MENSAJE_CAPACIDAD)
        @DecimalMax(value = "50", message = PublicacionRequestDTO.MENSAJE_CAPACIDAD)
        @Digits(integer = 2, fraction = 0, message = PublicacionRequestDTO.MENSAJE_CAPACIDAD)
        BigDecimal capacidad,

        // CA-021 y decisión 8: pesos sin decimales
        @NotNull(message = PublicacionRequestDTO.MENSAJE_PRECIO)
        @DecimalMin(value = "0", inclusive = false, message = PublicacionRequestDTO.MENSAJE_PRECIO)
        @Digits(integer = Publicacion.DIGITOS_MAXIMOS_PRECIO, fraction = 0,
                message = PublicacionRequestDTO.MENSAJE_PRECIO)
        BigDecimal precioPorNoche) {

    public static final String MENSAJE_TIPO =
            "El tipo de alojamiento debe ser uno de: APARTAMENTO, CASA, HABITACION, CABANA, OTRO";
    public static final String MENSAJE_CAPACIDAD = "La capacidad debe ser un número entero entre 1 y 50";
    public static final String MENSAJE_PRECIO =
            "El precio por noche debe ser un valor entero en pesos mayor que cero y de máximo 9 dígitos";

    // Ya validado: es uno de los valores del enum
    public TipoAlojamiento tipo() {
        return TipoAlojamiento.valueOf(tipoAlojamiento);
    }
}
