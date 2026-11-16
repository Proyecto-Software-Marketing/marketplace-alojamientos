package co.edu.unbosque.marketplace.reservas.dto;

import co.edu.unbosque.marketplace.reservas.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Respuesta de POST /bookings/{id}/cancel (Cancellation en el contrato)
public record CancellationResponseDTO(
        Long reservaId,
        Long canceladaPor,
        LocalDateTime fecha,
        String motivo,
        BigDecimal valorDevolucionSimulada,
        BookingStatus estadoFinal) {
}
