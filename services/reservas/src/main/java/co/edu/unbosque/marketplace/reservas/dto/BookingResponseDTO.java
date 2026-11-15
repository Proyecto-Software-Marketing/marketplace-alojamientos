package co.edu.unbosque.marketplace.reservas.dto;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Respuesta de POST /bookings (Booking en el contrato)
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

    public static BookingResponseDTO desde(Booking reserva) {
        return new BookingResponseDTO(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getPublicacionId(),
                reserva.getHuespedId(),
                reserva.getFechaLlegada(),
                reserva.getFechaSalida(),
                reserva.getCantidadHuespedes(),
                reserva.getMontoTotal(),
                reserva.getEstado(),
                reserva.getCreadaEn()
        );
    }
}
