package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// CA-067: dos clics simultáneos con la misma Idempotency-Key no deben duplicar la reserva
class BookingServiceIdempotenciaTest {

    private static final String TOKEN = "Bearer token-huesped";
    private static final String CLAVE = "idemp-carrera";
    private static final Long HUESPED_ID = 25L;
    private static final Long PUBLICACION_ID = 101L;
    private static final LocalDate LLEGADA = LocalDate.now().plusDays(5);
    private static final LocalDate SALIDA = LocalDate.now().plusDays(10);
    private static final BigDecimal TOTAL = new BigDecimal("450000.00");

    private BookingRepository bookingRepository;
    private ServiciosExternos serviciosExternos;
    private BookingService bookingService;

    @BeforeEach
    void preparar() {
        bookingRepository = mock(BookingRepository.class);
        serviciosExternos = mock(ServiciosExternos.class);

        PlatformTransactionManager gestor = mock(PlatformTransactionManager.class);
        when(gestor.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());

        bookingService = new BookingService(bookingRepository, serviciosExternos, gestor);

        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);
    }

    private BookingRequestDTO solicitud() {
        return new BookingRequestDTO("cot-abc-123", "pm_card_visa", PUBLICACION_ID,
                LLEGADA, SALIDA, 2, TOTAL);
    }

    private Booking reservaGanadora(Long huespedId) {
        return new Booking("RES-GANADORA", PUBLICACION_ID, huespedId, "cot-abc-123",
                LLEGADA, SALIDA, 2, TOTAL, "pm_card_visa", CLAVE,
                BookingStatus.CONFIRMADA, LocalDateTime.now());
    }

    @Test
    @DisplayName("CA-067 si otra solicitud simultánea gana la carrera, se devuelve la reserva ganadora")
    void carreraSimultaneaDevuelveLaReservaGanadora() {
        Booking ganadora = reservaGanadora(HUESPED_ID);
        when(bookingRepository.findByIdempotencyKey(CLAVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(ganadora));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("clave duplicada"));

        BookingResponseDTO resultado = bookingService.crearReserva(TOKEN, CLAVE, solicitud());

        assertEquals(ganadora.getCodigoReserva(), resultado.codigoReserva());
        verify(serviciosExternos, never()).bloquearPeriodo(any(), any(), any(), any());
        verify(serviciosExternos, never()).notificarReservaConfirmada(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("CA-067 la reserva ganadora de otro huésped no se expone")
    void carreraConClaveDeOtroHuespedSeRechaza() {
        when(bookingRepository.findByIdempotencyKey(CLAVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(reservaGanadora(99L)));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("clave duplicada"));

        assertThrows(PermisoException.class, () -> bookingService.crearReserva(TOKEN, CLAVE, solicitud()));
        verify(serviciosExternos, never()).bloquearPeriodo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CA-067 si la violación de clave no tiene reserva asociada, se propaga")
    void violacionSinReservaAsociadaSePropaga() {
        when(bookingRepository.findByIdempotencyKey(CLAVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("clave duplicada"));

        assertThrows(DataIntegrityViolationException.class,
                () -> bookingService.crearReserva(TOKEN, CLAVE, solicitud()));
    }
}
