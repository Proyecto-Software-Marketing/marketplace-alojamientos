package co.edu.unbosque.marketplace.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import co.edu.unbosque.marketplace.reservas.controller.InternalBookingController;
import co.edu.unbosque.marketplace.reservas.dto.EstadisticasReservasDTO;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;

// Pruebas de las rutas internas que usan Identidad y Reportes 
@SpringBootTest
class ConsultasInternasTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InternalBookingController internalBookingController;

    @BeforeEach
    void limpiar() {
        bookingRepository.deleteAll();
    }

    private Booking reserva(String clave, Long huesped, LocalDate llegada, BookingStatus estado, String monto) {
        return new Booking("COD-" + clave, 1L, huesped, 2L, "COT-" + clave, llegada, llegada.plusDays(2),
                2, new BigDecimal(monto), "pm_card_visa", clave, estado, LocalDateTime.now());
    }

    @Test
    void conReservaConfirmadaFuturaRespondeTrue() {
        bookingRepository.save(reserva("a", 5L, LocalDate.now().plusDays(10), BookingStatus.CONFIRMADA, "300000"));

        assertTrue(internalBookingController.tieneReservasFuturas(5L));
    }

    @Test
    void conReservasPasadasOCanceladasRespondeFalse() {
        bookingRepository.save(reserva("b", 5L, LocalDate.now().minusDays(10), BookingStatus.CONFIRMADA, "300000"));
        bookingRepository.save(reserva("c", 5L, LocalDate.now().plusDays(10), BookingStatus.CANCELADA, "300000"));

        assertFalse(internalBookingController.tieneReservasFuturas(5L));
    }

    @Test
    void cuentaLasReservasPorEstadoYLosIngresos() {
        bookingRepository.save(reserva("d", 5L, LocalDate.now().plusDays(10), BookingStatus.CONFIRMADA, "300000"));
        bookingRepository.save(reserva("e", 6L, LocalDate.now().minusDays(10), BookingStatus.COMPLETADA, "200000"));
        bookingRepository.save(reserva("f", 7L, LocalDate.now().plusDays(5), BookingStatus.CANCELADA, "100000"));

        EstadisticasReservasDTO estadisticas = internalBookingController.estadisticas();

        assertEquals(1L, estadisticas.porEstado().get("CONFIRMADA"));
        assertEquals(1L, estadisticas.porEstado().get("COMPLETADA"));
        assertEquals(1L, estadisticas.porEstado().get("CANCELADA"));
        // las canceladas no suman
        assertEquals(0, new BigDecimal("500000").compareTo(estadisticas.ingresosEstimados()));
        assertTrue(estadisticas.ciudadesMayorDemanda().isEmpty());
    }
}