package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.SolicitudInvalidaException;
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
import static org.mockito.Mockito.doThrow;
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

    private BookingRequestDTO solicitudCompleta() {
        return new BookingRequestDTO(COTIZACION_ID, "pm_card_visa", PUBLICACION_ID,
                LLEGADA, SALIDA, 2, TOTAL);
    }

    private BookingRequestDTO solicitudSoloCotizacion() {
        return new BookingRequestDTO(COTIZACION_ID, "pm_card_visa", null, null, null, null, null);
    }

    // ---------- CA-064 ----------

    @Test
    @DisplayName("CA-064 con una cotización de un periodo disponible, al confirmar se re-verifica y queda CONFIRMADA sin aprobación")
    void confirmaReservaInmediataSinAprobacionAnfitrion() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);

        BookingResponseDTO resultado = bookingService.crearReserva(TOKEN, "idemp-001", solicitudCompleta());

        verify(serviciosExternos).verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA);
        assertNotNull(resultado.id());
        assertTrue(resultado.codigoReserva().startsWith("RES-"));
        assertEquals(BookingStatus.CONFIRMADA, resultado.estado());
        assertEquals(HUESPED_ID, resultado.huespedId());
        assertEquals(PUBLICACION_ID, resultado.publicacionId());
        assertEquals(LLEGADA, resultado.fechaLlegada());
        assertEquals(SALIDA, resultado.fechaSalida());
        assertEquals(1, bookingRepository.count());
    }

    @Test
    @DisplayName("CA-064 si el request no trae los datos, se completan desde la cotización")
    void completaDatosDesdeLaCotizacion() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.consultarCotizacion(COTIZACION_ID))
                .thenReturn(new CotizacionDTO(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA, 2, TOTAL));
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);

        BookingResponseDTO resultado = bookingService.crearReserva(TOKEN, "idemp-cot", solicitudSoloCotizacion());

        assertEquals(PUBLICACION_ID, resultado.publicacionId());
        assertEquals(LLEGADA, resultado.fechaLlegada());
        assertEquals(SALIDA, resultado.fechaSalida());
        assertEquals(2, resultado.cantidadHuespedes());
        assertEquals(0, resultado.montoTotal().compareTo(TOTAL));
        assertEquals(BookingStatus.CONFIRMADA, resultado.estado());
    }

    // ---------- CA-065 ----------

    @Test
    @DisplayName("CA-065 si otro huésped reservó el mismo periodo mientras cotizaba, se rechaza y se informa")
    void rechazaReservaSiOtroHuespedYaReservoElPeriodo() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(false);

        ConflictoReservaException excepcion = assertThrows(ConflictoReservaException.class,
                () -> bookingService.crearReserva(TOKEN, "idemp-002", solicitudCompleta()));

        assertEquals("El alojamiento ya no está disponible para las fechas seleccionadas", excepcion.getMessage());
        assertEquals(0, bookingRepository.count());
        verify(serviciosExternos, never()).bloquearPeriodo(any(), any(), any(), any());
        verify(serviciosExternos, never()).notificarReservaConfirmada(any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("CA-065 si otra reserva gana la carrera al bloquear, la reserva local se revierte")
    void revierteReservaSiElBloqueoFallaPorCarrera() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);
        doThrow(new ConflictoReservaException(
                "El alojamiento ya no está disponible para las fechas seleccionadas"))
                .when(serviciosExternos).bloquearPeriodo(any(), any(), any(), any());

        assertThrows(ConflictoReservaException.class,
                () -> bookingService.crearReserva(TOKEN, "idemp-carrera", solicitudCompleta()));

        assertEquals(0, bookingRepository.count());
        verify(serviciosExternos, never()).notificarReservaConfirmada(any(), any(), any(), anyString());
    }

    // ---------- CA-066 ----------

    @Test
    @DisplayName("CA-066 al confirmar, el periodo queda bloqueado y se notifica al huésped y al anfitrión")
    void bloqueaPeriodoYGeneraNotificacionesAlConfirmar() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);
        when(serviciosExternos.consultarAnfitrion(PUBLICACION_ID)).thenReturn(88L);

        BookingResponseDTO resultado = bookingService.crearReserva(TOKEN, "idemp-003", solicitudCompleta());

        verify(serviciosExternos).bloquearPeriodo(eq(PUBLICACION_ID), eq(resultado.id()), eq(LLEGADA), eq(SALIDA));
        verify(serviciosExternos).notificarReservaConfirmada(
                eq(HUESPED_ID), eq(resultado.id()), eq(resultado.codigoReserva()), anyString());
        verify(serviciosExternos).notificarReservaConfirmada(
                eq(88L), eq(resultado.id()), eq(resultado.codigoReserva()), anyString());
    }

    // ---------- Idempotencia ----------

    @Test
    @DisplayName("RF-026: reintentar con la misma clave devuelve la misma reserva sin duplicar")
    void reintentoConMismaClaveDevuelveMismaReserva() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);

        BookingResponseDTO primera = bookingService.crearReserva(TOKEN, "idemp-repetida", solicitudCompleta());
        BookingResponseDTO segunda = bookingService.crearReserva(TOKEN, "idemp-repetida", solicitudCompleta());

        assertEquals(primera.id(), segunda.id());
        assertEquals(primera.codigoReserva(), segunda.codigoReserva());
        assertEquals(1, bookingRepository.count());
        verify(serviciosExternos, times(1)).bloquearPeriodo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("La misma clave usada por otro huésped se rechaza")
    void claveDeOtroHuespedSeRechaza() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);
        bookingService.crearReserva(TOKEN, "idemp-ajena", solicitudCompleta());

        when(serviciosExternos.verificarRolHuesped("Bearer otro")).thenReturn(99L);

        assertThrows(PermisoException.class,
                () -> bookingService.crearReserva("Bearer otro", "idemp-ajena", solicitudCompleta()));
    }

    // ---------- Validaciones ----------

    @Test
    @DisplayName("Sin clave de idempotencia no se crea la reserva")
    void sinClaveDeIdempotenciaFalla() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);

        assertThrows(SolicitudInvalidaException.class,
                () -> bookingService.crearReserva(TOKEN, " ", solicitudCompleta()));
        assertEquals(0, bookingRepository.count());
    }

    @Test
    @DisplayName("Si la salida no es posterior a la llegada se rechaza")
    void fechasInvalidasSeRechazan() {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);

        BookingRequestDTO solicitud = new BookingRequestDTO(COTIZACION_ID, "pm_card_visa", PUBLICACION_ID,
                SALIDA, LLEGADA, 2, TOTAL);

        assertThrows(FechasInvalidasException.class,
                () -> bookingService.crearReserva(TOKEN, "idemp-fechas", solicitud));
    }
}
