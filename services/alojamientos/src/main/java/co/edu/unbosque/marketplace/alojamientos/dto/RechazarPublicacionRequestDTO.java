package co.edu.unbosque.marketplace.alojamientos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarPublicacionRequestDTO(
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(min = 3, max = 2000,
              message = "El motivo debe tener entre 3 y 2000 caracteres")
        String motivo
) {
}