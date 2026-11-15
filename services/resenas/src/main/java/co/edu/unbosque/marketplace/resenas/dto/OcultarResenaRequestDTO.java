package co.edu.unbosque.marketplace.resenas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cuerpo de POST /admin/reviews/{id}/hide (ReasonInput en el contrato). Decisión 2: motivo de 3 a 500 caracteres
public record OcultarResenaRequestDTO(
        @NotBlank(message = OcultarResenaRequestDTO.MENSAJE_MOTIVO)
        @Size(min = 3, max = 500, message = OcultarResenaRequestDTO.MENSAJE_MOTIVO)
        String motivo) {

    public static final String MENSAJE_MOTIVO = "El motivo es obligatorio y debe tener entre 3 y 500 caracteres";
}
