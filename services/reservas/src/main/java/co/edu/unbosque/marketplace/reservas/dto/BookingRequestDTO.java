package co.edu.unbosque.marketplace.reservas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// Cuerpo de POST /bookings (BookingRequest en el contrato).
// La cotización y el método de pago son obligatorios. Los demás campos son un
// snapshot opcional: si no llegan, se completan consultando la cotización.
public record BookingRequestDTO(
        @NotBlank(message = "La cotización es obligatoria")
        @Size(max = 100, message = "La cotización no es válida")
        String cotizacionId,

        @NotBlank(message = "El método de pago es obligatorio")
        @Size(max = 100, message = "El método de pago no es válido")
        String metodoPago,

        Long publicacionId,

        LocalDate fechaLlegada,

        LocalDate fechaSalida,

        @Positive(message = "La cantidad de huéspedes debe ser mayor a 0")
        Integer cantidadHuespedes,

        @DecimalMin(value = "0.0", message = "El monto total no puede ser negativo")
        BigDecimal montoTotal) {
}
