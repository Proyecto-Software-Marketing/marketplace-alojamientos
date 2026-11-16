package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.PoliticaCancelacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.client.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.reservas.dto.CancelRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.CancellationResponseDTO;
import co.edu.unbosque.marketplace.reservas.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la HU-28: cancelación con devolución simulada, permisos,
// liberación del periodo y notificaciones.
@SpringBootTest
class BookingServiceCancelacionTest {

    private static final String TOKEN_HUESPED = "Bearer token-huesped";
    private static final String TOKEN_ADMIN = "Bearer token-admin";
    private static final Long HUESPED_ID = 25L;
    private static final Long OTRO_HUESPED_ID = 99L;
    private static final Long ADMIN_ID = 1L;
    private static final Long ANFITRION_ID = 88L;
    private static final Long PUBLICACION_ID = 101L;
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

    private Booking reservaGuardada(Long huespedId, Long anfitrionId, BookingStatus estado, String clave) {
        return bookingRepository.save(new Booking(
                "RES-" + clave.toUpperCase(),
                PUBLICACION_ID,
                huespedId,
                anfitrionId,
                "cot-abc-123",
                LLEGADA,
                SALIDA,
                2,
                TOTAL,
                "pm_card_visa",
                clave,
                estado,
                LocalDateTime.now()));
    }

    private CancelRequestDTO solicitud() {
        return new CancelRequestDTO("  Viaje cancelado  ");
    }

    private void autorizarHuesped() {
        when(serviciosExternos.verificarSesionCancelacion(TOKEN_HUESPED))
                .thenReturn(new UsuarioIdentidadDTO(HUESPED_ID, "HUESPED"));
    }

    @Test
    @DisplayName("CA-077 el huésped cancela su reserva y ve la devolución calculada por la política")
    void huespedCancelaSuReservaConDevolucion() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "cancelacion-ok");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Flexible", new BigDecimal("50"), true));

        CancellationResponseDTO resultado = bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud());

        assertEquals(reserva.getId(), resultado.reservaId());
        assertEquals(HUESPED_ID, resultado.canceladaPor());
        assertEquals("Viaje cancelado", resultado.motivo());
        assertEquals(0, resultado.valorDevolucionSimulada().compareTo(new BigDecimal("225000.00")));
        assertEquals(BookingStatus.CANCELADA, resultado.estadoFinal());

        Booking guardada = bookingRepository.findById(reserva.getId()).orElseThrow();
        assertEquals(BookingStatus.CANCELADA, guardada.getEstado());
        assertEquals(HUESPED_ID, guardada.getCanceladaPor());
        assertEquals("Viaje cancelado", guardada.getMotivoCancelacion());
        assertEquals(0, guardada.getValorDevolucionSimulada().compareTo(new BigDecimal("225000.00")));

        verify(serviciosExternos).liberarPeriodo(PUBLICACION_ID, reserva.getId(), LLEGADA, SALIDA);
        verify(serviciosExternos).notificarReservaCancelada(
                eq(HUESPED_ID), eq(reserva.getId()), eq(reserva.getCodigoReserva()), anyString());
        verify(serviciosExternos).notificarReservaCancelada(
                eq(ANFITRION_ID), eq(reserva.getId()), eq(reserva.getCodigoReserva()), anyString());

        ArgumentCaptor<EventoAuditoriaDTO> evento = ArgumentCaptor.forClass(EventoAuditoriaDTO.class);
        verify(serviciosExternos).registrarAuditoria(evento.capture());
        String detalle = "Fecha de cancelación: " + resultado.fecha()
                + "; Motivo: Viaje cancelado"
                + "; Devolución simulada: 225000.00"
                + "; Estado final: CANCELADA";
        assertEquals(new EventoAuditoriaDTO(HUESPED_ID, "RESERVA", reserva.getId(),
                "CANCELAR_RESERVA", "EXITO", detalle), evento.getValue());
    }

    @Test
    @DisplayName("CA-082 si Auditoría falla, la cancelación igual queda registrada")
    void auditoriaCaidaNoRevierteLaCancelacion() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "auditoria-caida");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Flexible", new BigDecimal("50"), false));
        when(serviciosExternos.registrarAuditoria(any())).thenReturn(false);

        CancellationResponseDTO resultado = bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud());

        assertEquals(BookingStatus.CANCELADA, resultado.estadoFinal());
        assertEquals(BookingStatus.CANCELADA,
                bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
        verify(serviciosExternos).registrarAuditoria(any());
    }

    @Test
    @DisplayName("CA-078 el administrador cancela una reserva ajena")
    void administradorCancelaReservaAjena() {
        Booking reserva = reservaGuardada(OTRO_HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "cancelacion-admin");
        when(serviciosExternos.verificarSesionCancelacion(TOKEN_ADMIN))
                .thenReturn(new UsuarioIdentidadDTO(ADMIN_ID, "ADMINISTRADOR"));
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Estricta", new BigDecimal("100"), false));

        CancellationResponseDTO resultado = bookingService.cancelarReserva(TOKEN_ADMIN, reserva.getId(), solicitud());

        assertEquals(ADMIN_ID, resultado.canceladaPor());
        assertEquals(0, resultado.valorDevolucionSimulada().compareTo(TOTAL));
        assertEquals(BookingStatus.CANCELADA, resultado.estadoFinal());

        verify(serviciosExternos, never()).liberarPeriodo(any(), any(), any(), any());
        verify(serviciosExternos).notificarReservaCancelada(
                eq(OTRO_HUESPED_ID), eq(reserva.getId()), eq(reserva.getCodigoReserva()), anyString());
        verify(serviciosExternos).notificarReservaCancelada(
                eq(ANFITRION_ID), eq(reserva.getId()), eq(reserva.getCodigoReserva()), anyString());
    }

    @Test
    @DisplayName("CA-079 el anfitrión no puede cancelar")
    void anfitrionNoPuedeCancelar() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "cancelacion-host");
        when(serviciosExternos.verificarSesionCancelacion("Bearer token-anfitrion"))
                .thenThrow(new PermisoException("No tiene permiso para cancelar esta reserva"));

        assertThrows(PermisoException.class,
                () -> bookingService.cancelarReserva("Bearer token-anfitrion", reserva.getId(), solicitud()));

        assertEquals(BookingStatus.CONFIRMADA,
                bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
        verify(serviciosExternos, never()).consultarPoliticaCancelacion(any());
        verify(serviciosExternos, never()).notificarReservaCancelada(any(), any(), anyString(), anyString());
        verify(serviciosExternos, never()).registrarAuditoria(any());
    }

    @Test
    @DisplayName("Un huésped no puede cancelar una reserva ajena")
    void huespedNoPuedeCancelarReservaAjena() {
        Booking reserva = reservaGuardada(OTRO_HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "cancelacion-ajena");
        autorizarHuesped();

        assertThrows(PermisoException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud()));

        assertEquals(BookingStatus.CONFIRMADA,
                bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
        verify(serviciosExternos, never()).consultarPoliticaCancelacion(any());
    }

    @Test
    @DisplayName("Cancelar una reserva inexistente responde no encontrado")
    void reservaInexistenteNoSeEncuentra() {
        autorizarHuesped();

        assertThrows(NoEncontradoException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, 999999L, solicitud()));

        verify(serviciosExternos, never()).consultarPoliticaCancelacion(any());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"CANCELADA", "COMPLETADA", "EN_REVISION_MANUAL"})
    @DisplayName("Solo una reserva CONFIRMADA se puede cancelar")
    void estadoNoPermitidoNoSeCancela(BookingStatus estado) {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, estado, "estado-" + estado.name());
        autorizarHuesped();

        assertThrows(ConflictoReservaException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud()));

        assertEquals(estado, bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
        verify(serviciosExternos, never()).consultarPoliticaCancelacion(any());
        verify(serviciosExternos, never()).liberarPeriodo(any(), any(), any(), any());
        verify(serviciosExternos, never()).registrarAuditoria(any());
    }

    @ParameterizedTest
    @CsvSource({"0,0.00", "50,225000.00", "100,450000.00", "33.33,149985.00"})
    @DisplayName("La devolución se calcula con el porcentaje de la política")
    void devolucionSegunPorcentaje(String porcentaje, String esperado) {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA,
                "porcentaje-" + porcentaje.replace(".", "-"));
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Política", new BigDecimal(porcentaje), true));

        CancellationResponseDTO resultado = bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud());

        assertEquals(0, resultado.valorDevolucionSimulada().compareTo(new BigDecimal(esperado)));
    }

    @Test
    @DisplayName("CA-080 si la política no permite liberar, el periodo permanece bloqueado")
    void politicaSinLiberacionNoLlamaDisponibilidad() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "sin-liberacion");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Moderada", new BigDecimal("50"), false));

        bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud());

        verify(serviciosExternos, never()).liberarPeriodo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CA-080 si falla la liberación, se revierte la cancelación")
    void falloAlLiberarPeriodoRevierteLaCancelacion() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "fallo-liberacion");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Flexible", new BigDecimal("50"), true));
        doThrow(new ServicioNoDisponibleException("No fue posible liberar el periodo. Intente más tarde"))
                .when(serviciosExternos).liberarPeriodo(any(), any(), any(), any());

        assertThrows(ServicioNoDisponibleException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud()));

        Booking guardada = bookingRepository.findById(reserva.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMADA, guardada.getEstado());
        assertNull(guardada.getCanceladaPor());
        assertNull(guardada.getFechaCancelacion());
        verify(serviciosExternos, never()).notificarReservaCancelada(any(), any(), anyString(), anyString());
        verify(serviciosExternos, never()).registrarAuditoria(any());
    }

    @Test
    @DisplayName("CA-128 si la reserva no tiene anfitrión, se consulta para notificarlo")
    void notificaAnfitrionConsultadoCuandoNoEstaEnLaReserva() {
        Booking reserva = reservaGuardada(HUESPED_ID, null, BookingStatus.CONFIRMADA, "host-consultado");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Flexible", new BigDecimal("50"), false));
        when(serviciosExternos.consultarAnfitrion(PUBLICACION_ID)).thenReturn(ANFITRION_ID);

        bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud());

        verify(serviciosExternos).consultarAnfitrion(PUBLICACION_ID);
        verify(serviciosExternos).notificarReservaCancelada(
                eq(ANFITRION_ID), eq(reserva.getId()), eq(reserva.getCodigoReserva()), anyString());
    }

    @Test
    @DisplayName("Si no se puede obtener la política, no se cancela")
    void politicaNoDisponibleNoCancela() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "sin-politica");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenThrow(new ServicioNoDisponibleException(
                        "No fue posible consultar la política de cancelación. Intente más tarde"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud()));

        assertEquals(BookingStatus.CONFIRMADA,
                bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
    }

    @ParameterizedTest
    @MethodSource("porcentajesInvalidos")
    @DisplayName("Una política con porcentaje inválido no permite cancelar")
    void politicaConPorcentajeInvalidoNoCancela(BigDecimal porcentaje) {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA,
                "porcentaje-invalido-" + (porcentaje == null ? "nulo" : porcentaje.toPlainString().replace(".", "-")));
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID))
                .thenReturn(new PoliticaCancelacionDTO("Inválida", porcentaje, true));

        assertThrows(ServicioNoDisponibleException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud()));

        assertEquals(BookingStatus.CONFIRMADA,
                bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
        verify(serviciosExternos, never()).liberarPeriodo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Una política nula no permite cancelar")
    void politicaNulaNoCancela() {
        Booking reserva = reservaGuardada(HUESPED_ID, ANFITRION_ID, BookingStatus.CONFIRMADA, "politica-nula");
        autorizarHuesped();
        when(serviciosExternos.consultarPoliticaCancelacion(PUBLICACION_ID)).thenReturn(null);

        assertThrows(ServicioNoDisponibleException.class,
                () -> bookingService.cancelarReserva(TOKEN_HUESPED, reserva.getId(), solicitud()));

        assertEquals(BookingStatus.CONFIRMADA,
                bookingRepository.findById(reserva.getId()).orElseThrow().getEstado());
    }

    private static Stream<BigDecimal> porcentajesInvalidos() {
        return Stream.of(null, new BigDecimal("-1"), new BigDecimal("101"));
    }
}
