package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la HU-23 Reserva inmediata (CA-064, CA-065, CA-066)
@SpringBootTest
class BookingServiceTest {

    private static final String TOKEN = "Bearer token-huesped-valido";
    private static final Long HUESPED_ID = 25L;
    private static final Long PUBLICACION_ID = 101L;
    private static final String COTIZACION_ID = "cot-abc-123";
    private static final LocalDate LLEGADA = LocalDate.now().plusDays(5);
    private static final LocalDate SALIDA = LocalDate.now().plusDays(10);
    private static final BigDecimal TOTAL = new BigDecimal("450000.00");

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void limpiar() {
        bookingRepository.deleteAll();
    }

    // ---------- CA-064 ----------
    @Test
    @DisplayName("CA-064 dado que tengo una cotización de un periodo disponible, cuando confirmo, vuelve a verificar disponibilidad y queda CONFIRMADA sin aprobación del anfitrión")
    void confirmaReservaInmediataSinAprobacionAnfitrion() {
        // Dado que tengo una cotización de un periodo disponible
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.consultarCotizacion(COTIZACION_ID))
                .thenReturn(new CotizacionDTO(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA, 2, TOTAL));
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);

        BookingRequestDTO request = new BookingRequestDTO(
                COTIZACION_ID,
                "pm_card_visa",
                PUBLICACION_ID,
                LLEGADA,
                SALIDA,
                2,
                TOTAL
        );

        // Cuando confirmo la reserva
        BookingResponseDTO resultado = bookingService.crearReserva(TOKEN, "idemp-001", request);

        // Entonces el sistema vuelve a verificar la disponibilidad
        verify(serviciosExternos).verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA);

        // Y la reserva queda CONFIRMADA sin requerir aprobación del anfitrión
        assertNotNull(resultado);
        assertNotNull(resultado.id());
        assertNotNull(resultado.codigoReserva());
        assertTrue(resultado.codigoReserva().startsWith("RES-"));
        assertEquals(BookingStatus.CONFIRMADA, resultado.estado());
        assertEquals(HUESPED_ID, resultado.huespedId());
        assertEquals(PUBLICACION_ID, resultado.publicacionId());
        assertEquals(LLEGADA, resultado.fechaLlegada());
        assertEquals(SALIDA, resultado.fechaSalida());
    }

    // ---------- CA-065 ----------
    @Test
    @DisplayName("CA-065 dado que otro huésped reservó el mismo periodo mientras cotizaba, cuando confirmo, el sistema rechaza e informa que ya no está disponible")
    void rechazaReservaSiOtroHuespedYaReservoElPeriodo() {
        // Dado que otro huésped reservó el mismo periodo mientras yo cotizaba
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.consultarCotizacion(COTIZACION_ID))
                .thenReturn(new CotizacionDTO(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA, 2, TOTAL));
        // Disponibilidad responde falso porque ya no está libre
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(false);

        BookingRequestDTO request = new BookingRequestDTO(
                COTIZACION_ID,
                "pm_card_visa",
                PUBLICACION_ID,
                LLEGADA,
                SALIDA,
                2,
                TOTAL
        );

        // Cuando confirmo
        ConflictoReservaException excepcion = assertThrows(ConflictoReservaException.class, () ->
                bookingService.crearReserva(TOKEN, "idemp-002", request)
        );

        // Entonces el sistema rechaza mi reserva e informa que ya no está disponible
        assertEquals("El alojamiento ya no está disponible para las fechas seleccionadas", excepcion.getMessage());

        // Y no se crea ninguna reserva en la base de datos
        assertEquals(0, bookingRepository.count());

        // Y no se bloquea periodo ni se generan notificaciones
        verify(serviciosExternos, never()).bloquearPeriodo(any(), any(), any(), any());
        verify(serviciosExternos, never()).notificarReservaConfirmada(any(), any(), any());
    }

    // ---------- CA-066 ----------
    @Test
    @DisplayName("CA-066 dado que mi reserva queda confirmada, cuando termina el proceso, el periodo queda bloqueado y se generan las notificaciones correspondientes")
    void bloqueaPeriodoYGeneraNotificacionesAlConfirmar() {
        // Dado que la reserva puede confirmarse
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);
        when(serviciosExternos.consultarAnfitrion(PUBLICACION_ID)).thenReturn(88L);

        BookingRequestDTO request = new BookingRequestDTO(
                COTIZACION_ID,
                "pm_card_visa",
                PUBLICACION_ID,
                LLEGADA,
                SALIDA,
                2,
                TOTAL
        );

        // Cuando la reserva queda confirmada
        BookingResponseDTO resultado = bookingService.crearReserva(TOKEN, "idemp-003", request);

        // Entonces el periodo queda bloqueado en Disponibilidad
        verify(serviciosExternos).bloquearPeriodo(eq(PUBLICACION_ID), eq(resultado.id()), eq(LLEGADA), eq(SALIDA));

        // Y se generan las notificaciones correspondientes al huésped y al anfitrión
        verify(serviciosExternos).notificarReservaConfirmada(eq(HUESPED_ID), eq(resultado.id()), eq(resultado.codigoReserva()));
        verify(serviciosExternos).notificarReservaConfirmada(eq(88L), eq(resultado.id()), eq(resultado.codigoReserva()));
    }

    // ---------- Idempotencia ----------
    @Test
    @DisplayName("Reintentar con la misma clave de idempotencia devuelve la misma reserva sin duplicar")
    void reintentoConMismaClaveDevuelveMismaReserva() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);

        BookingRequestDTO request = new BookingRequestDTO(
                COTIZACION_ID,
                "pm_card_visa",
                PUBLICACION_ID,
                LLEGADA,
                SALIDA,
                2,
                TOTAL
        );

        BookingResponseDTO primera = bookingService.crearReserva(TOKEN, "idemp-repetida", request);
        BookingResponseDTO segunda = bookingService.crearReserva(TOKEN, "idemp-repetida", request);

        assertEquals(primera.id(), segunda.id());
        assertEquals(primera.codigoReserva(), segunda.codigoReserva());
        assertEquals(1, bookingRepository.count());
        // Solo se bloqueó y notificó una vez
        verify(serviciosExternos, times(1)).bloquearPeriodo(any(), any(), any(), any());
    }
}
