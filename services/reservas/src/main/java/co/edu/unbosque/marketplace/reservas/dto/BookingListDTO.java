package co.edu.unbosque.marketplace.reservas.dto;

import java.util.List;

public record BookingListDTO(
        List<BookingResponseDTO> items) {
}
