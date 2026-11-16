package co.edu.unbosque.marketplace.reservas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.reservas.dto.EstadisticasReservasDTO;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;

@Service
public class ConsultasInternasService {

    private final BookingRepository bookingRepository;

    public ConsultasInternasService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }


    public boolean tieneReservasFuturas(Long idUsuario) {
        return bookingRepository.existsByHuespedIdAndEstadoAndFechaLlegadaAfter(
                idUsuario, BookingStatus.CONFIRMADA, LocalDate.now());
    }

   
    public EstadisticasReservasDTO estadisticas() {
        Map<String, Long> porEstado = new TreeMap<>();
        BigDecimal ingresos = BigDecimal.ZERO;
        for (Booking reserva : bookingRepository.findAll()) {
            porEstado.merge(reserva.getEstado().name(), 1L, Long::sum);
            if (reserva.getEstado() == BookingStatus.CONFIRMADA || reserva.getEstado() == BookingStatus.COMPLETADA) {
                ingresos = ingresos.add(reserva.getMontoTotal());
            }
        }
       
        return new EstadisticasReservasDTO(porEstado, ingresos, List.of());
    }
}