package co.edu.unbosque.marketplace.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cuerpo de POST /bookings/{id}/cancel (CancelRequest en el contrato)
public record CancelRequestDTO(
        @NotBlank(message = "El motivo de cancelación es obligatorio")
        @Size(min = 3, max = 500, message = "El motivo debe tener entre 3 y 500 caracteres")
        String motivo) {

    public CancelRequestDTO {
        motivo = motivo == null ? null : motivo.trim();
    }
}
