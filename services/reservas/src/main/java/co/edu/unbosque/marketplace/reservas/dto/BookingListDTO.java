package co.edu.unbosque.marketplace.reservas.dto;

import java.util.List;

// Respuesta de GET /bookings (BookingList en el contrato)
public record BookingListDTO(List<BookingResponseDTO> items) {
}
