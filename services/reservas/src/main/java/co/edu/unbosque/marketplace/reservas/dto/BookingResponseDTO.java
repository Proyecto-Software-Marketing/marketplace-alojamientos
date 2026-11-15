package co.edu.unbosque.marketplace.reservas.dto;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponseDTO(
        Long id,
        String codigoReserva,
        Long publicacionId,
        Long huespedId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal montoTotal,
        BookingStatus estado,
        LocalDateTime creadaEn) {

    public static BookingResponseDTO desde(Booking b) {
        return new BookingResponseDTO(
                b.getId(),
                b.getCodigoReserva(),
                b.getPublicacionId(),
                b.getHuespedId(),
                b.getFechaLlegada(),
                b.getFechaSalida(),
                b.getCantidadHuespedes(),
                b.getMontoTotal(),
                b.getEstado(),
                b.getCreadaEn()
        );
    }
}
