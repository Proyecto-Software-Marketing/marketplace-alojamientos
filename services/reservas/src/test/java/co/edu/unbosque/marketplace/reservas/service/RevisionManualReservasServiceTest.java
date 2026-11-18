package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisionManualReservasServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    private RevisionManualReservasService servicio;

    @BeforeEach
    void configurar() {
        servicio = new RevisionManualReservasService(bookingRepository);
    }

    @Test
    void cambiaReservasConfirmadasARevisionManual() {
        Booking primera = mock(Booking.class);
        Booking segunda = mock(Booking.class);

        when(bookingRepository
                .findByPublicacionIdAndEstadoAndFechaSalidaAfter(
                        eq(15L),
                        eq(BookingStatus.CONFIRMADA),
                        any(LocalDate.class)))
                .thenReturn(List.of(primera, segunda));

        int cantidad = servicio.revisarReservas(15L);

        assertEquals(2, cantidad);

        verify(primera).setEstado(BookingStatus.EN_REVISION_MANUAL);
        verify(segunda).setEstado(BookingStatus.EN_REVISION_MANUAL);
        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void sinReservasAfectadasDevuelveCero() {
        when(bookingRepository
                .findByPublicacionIdAndEstadoAndFechaSalidaAfter(
                        eq(15L),
                        eq(BookingStatus.CONFIRMADA),
                        any(LocalDate.class)))
                .thenReturn(List.of());

        int cantidad = servicio.revisarReservas(15L);

        assertEquals(0, cantidad);
        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void identificadorInvalidoEsRechazado() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.revisarReservas(-1L));

        verifyNoInteractions(bookingRepository);
    }
}