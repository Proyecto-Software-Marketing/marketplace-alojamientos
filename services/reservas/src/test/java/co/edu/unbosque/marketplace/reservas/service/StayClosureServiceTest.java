package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.StayClosureResultDTO;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Pruebas de la HU-27 Cierre automático de estancias (CA-075, CA-076)
@SpringBootTest
class StayClosureServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    // "Hoy" fijo para las pruebas: 20 de noviembre de 2026 a las 00:05 en Bogotá
    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    @Autowired
    private StayClosureService stayClosureService;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private Clock reloj;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private int secuencia;

    @BeforeEach
    void preparar() {
        bookingRepository.deleteAll();
        when(reloj.instant()).thenReturn(ZonedDateTime.of(HOY.atTime(0, 5), BOGOTA).toInstant());
        when(reloj.getZone()).thenReturn(BOGOTA);
    }

    private Booking reserva(BookingStatus estado, LocalDate salida) {
        secuencia++;
        return bookingRepository.save(new Booking("RES-CIERRE" + secuencia, 101L, 25L, 88L, "cot-" + secuencia,
                salida.minusDays(3), salida, 2, new BigDecimal("450000.00"), "pm_card_visa",
                "idemp-cierre-" + secuencia, estado, LocalDateTime.of(2026, 11, 1, 10, 0)));
    }

    private BookingStatus estadoActual(Booking reserva) {
        return bookingRepository.findById(reserva.getId()).orElseThrow().getEstado();
    }

    @Test
    @DisplayName("CA-075 una reserva CONFIRMADA cuya fecha de salida ya pasó pasa a COMPLETADA al ejecutar el cierre")
    void completaReservaConfirmadaConSalidaPasada() {
        // Dado una reserva CONFIRMADA que salió ayer y otra que salió hace un mes
        Booking ayer = reserva(BookingStatus.CONFIRMADA, HOY.minusDays(1));
        Booking haceUnMes = reserva(BookingStatus.CONFIRMADA, HOY.minusMonths(1));

        // Cuando el sistema ejecuta el cierre de estancias
        StayClosureResultDTO resultado = stayClosureService.cerrarEstanciasTerminadas();

        // Entonces ambas pasan a COMPLETADA
        assertEquals(2, resultado.reservasCompletadas());
        assertEquals(BookingStatus.COMPLETADA, estadoActual(ayer));
        assertEquals(BookingStatus.COMPLETADA, estadoActual(haceUnMes));
    }

    @Test
    @DisplayName("CA-076 una reserva CANCELADA no cambia a COMPLETADA aunque pase su fecha de salida")
    void noCompletaReservaCancelada() {
        // Dado una reserva CANCELADA cuya fecha de salida ya pasó
        Booking cancelada = reserva(BookingStatus.CANCELADA, HOY.minusDays(2));

        // Cuando el sistema ejecuta el cierre de estancias
        StayClosureResultDTO resultado = stayClosureService.cerrarEstanciasTerminadas();

        // Entonces sigue CANCELADA
        assertEquals(0, resultado.reservasCompletadas());
        assertEquals(BookingStatus.CANCELADA, estadoActual(cancelada));
    }

    @Test
    @DisplayName("Decisión 1: una reserva CONFIRMADA que sale hoy o en el futuro no se cierra todavía")
    void noCompletaReservaQueSaleHoyOEnElFuturo() {
        // Dado una reserva que sale hoy y otra que sale mañana
        Booking saleHoy = reserva(BookingStatus.CONFIRMADA, HOY);
        Booking saleManana = reserva(BookingStatus.CONFIRMADA, HOY.plusDays(1));

        // Cuando el sistema ejecuta el cierre de estancias
        StayClosureResultDTO resultado = stayClosureService.cerrarEstanciasTerminadas();

        // Entonces ambas siguen CONFIRMADA
        assertEquals(0, resultado.reservasCompletadas());
        assertEquals(BookingStatus.CONFIRMADA, estadoActual(saleHoy));
        assertEquals(BookingStatus.CONFIRMADA, estadoActual(saleManana));
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"EN_REVISION_MANUAL", "COMPLETADA"})
    @DisplayName("Decisión 3: solo cambian las CONFIRMADA; los demás estados con salida pasada se quedan igual")
    void noCambiaOtrosEstados(BookingStatus estado) {
        // Dado una reserva en otro estado cuya fecha de salida ya pasó
        Booking otra = reserva(estado, HOY.minusDays(5));

        // Cuando el sistema ejecuta el cierre de estancias
        StayClosureResultDTO resultado = stayClosureService.cerrarEstanciasTerminadas();

        // Entonces conserva su estado
        assertEquals(0, resultado.reservasCompletadas());
        assertEquals(estado, estadoActual(otra));
    }

    @Test
    @DisplayName("Decisión 4: el cierre es idempotente, la segunda ejecución no cambia nada")
    void cierreIdempotente() {
        // Dado una reserva CONFIRMADA que ya terminó y fue cerrada una vez
        Booking terminada = reserva(BookingStatus.CONFIRMADA, HOY.minusDays(1));
        assertEquals(1, stayClosureService.cerrarEstanciasTerminadas().reservasCompletadas());

        // Cuando el cierre se ejecuta de nuevo
        StayClosureResultDTO segunda = stayClosureService.cerrarEstanciasTerminadas();

        // Entonces no cambia nada más
        assertEquals(0, segunda.reservasCompletadas());
        assertEquals(BookingStatus.COMPLETADA, estadoActual(terminada));
    }

    @Test
    @DisplayName("Decisión 5: el cierre no llama a otros servicios (sin notificación ni liberación en Disponibilidad)")
    void noLlamaOtrosServicios() {
        // Dado una reserva CONFIRMADA que ya terminó
        reserva(BookingStatus.CONFIRMADA, HOY.minusDays(1));

        // Cuando el sistema ejecuta el cierre de estancias
        stayClosureService.cerrarEstanciasTerminadas();

        // Entonces no se llama a ningún otro servicio
        verifyNoInteractions(serviciosExternos);
    }
}
