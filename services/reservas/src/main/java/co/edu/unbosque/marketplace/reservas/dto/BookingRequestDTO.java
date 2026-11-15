package co.edu.unbosque.marketplace.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequestDTO(
        @NotBlank(message = "La cotización es obligatoria") String cotizacionId,
        @NotBlank(message = "El método de pago es obligatorio") String metodoPago,
        Long publicacionId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal montoTotal) {
}
