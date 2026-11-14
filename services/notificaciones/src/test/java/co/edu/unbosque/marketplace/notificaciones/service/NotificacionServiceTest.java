package co.edu.unbosque.marketplace.notificaciones.service;

import co.edu.unbosque.marketplace.notificaciones.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEntidad;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEvento;
import co.edu.unbosque.marketplace.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// Pruebas de la HU-30: generación de notificaciones
@SpringBootTest
class NotificacionServiceTest {

    private static final Long ANFITRION = 7L;
    private static final Long HUESPED = 21L;
    private static final Long PUBLICACION = 15L;
    private static final Long RESERVA = 300L;

    // Hora fija del reloj: 14 de noviembre de 2026, 6:05 p. m. en Bogotá
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-11-14T23:05:00Z");

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private Clock reloj;

    @BeforeEach
    void preparar() {
        notificacionRepository.deleteAll();
        when(reloj.instant()).thenReturn(AHORA);
        when(reloj.getZone()).thenReturn(BOGOTA);
    }

    // ---------- CA-083 ----------

    @Test
    @DisplayName("CA-083 al aprobarse una publicación se registra una notificación para el anfitrión")
    void publicacionAprobadaNotificaAlAnfitrion() {
        // Dado que Alojamientos aprueba la publicación 15 del anfitrión 7
        // Cuando avisa el evento
        notificacionService.registrar(ANFITRION, "PUBLICACION_APROBADA", null, PUBLICACION, null);

        // Entonces queda una notificación para el anfitrión
        List<Notificacion> generadas = notificacionRepository
                .findByTipoEventoAndIdEntidad(TipoEvento.PUBLICACION_APROBADA, PUBLICACION);
        assertEquals(1, generadas.size());
        assertEquals(ANFITRION, generadas.get(0).getIdUsuario());
    }

    @Test
    @DisplayName("CA-083 al confirmarse una reserva se registra una notificación para el huésped y otra para el anfitrión")
    void reservaConfirmadaNotificaAHuespedYAnfitrion() {
        // Dado que Reservas confirma la reserva 300
        // Cuando avisa el evento a cada usuario afectado
        notificacionService.registrar(HUESPED, "RESERVA_CONFIRMADA", "RESERVA", RESERVA, "Tu reserva fue confirmada");
        notificacionService.registrar(ANFITRION, "RESERVA_CONFIRMADA", "RESERVA", RESERVA,
                "Recibiste una reserva confirmada");

        // Entonces hay una notificación por cada usuario
        List<Long> usuarios = notificacionRepository
                .findByTipoEventoAndIdEntidad(TipoEvento.RESERVA_CONFIRMADA, RESERVA).stream()
                .map(Notificacion::getIdUsuario)
                .sorted()
                .toList();
        assertEquals(List.of(ANFITRION, HUESPED), usuarios);
    }

    // ---------- CA-084 ----------

    @Test
    @DisplayName("CA-084 la notificación incluye el tipo de evento, la fecha y la entidad relacionada")
    void notificacionIncluyeTipoFechaYEntidad() {
        // Dado un evento de reserva confirmada
        // Cuando se registra la notificación
        Notificacion guardada = notificacionService
                .registrar(HUESPED, "RESERVA_CONFIRMADA", null, RESERVA, null).notificacion();

        // Entonces trae tipo de evento, fecha y entidad relacionada, y queda sin leer
        Notificacion leida = notificacionRepository.findById(guardada.getId()).orElseThrow();
        assertEquals(TipoEvento.RESERVA_CONFIRMADA, leida.getTipoEvento());
        assertEquals(LocalDateTime.of(2026, 11, 14, 18, 5), leida.getFechaCreacion());
        assertEquals(TipoEntidad.RESERVA, leida.getEntidadTipo());
        assertEquals(RESERVA, leida.getIdEntidad());
        assertFalse(leida.isLeida());
    }

    @Test
    @DisplayName("CA-084 la publicación aprobada queda relacionada con una entidad de tipo PUBLICACION")
    void publicacionAprobadaRelacionaPublicacion() {
        Notificacion guardada = notificacionService
                .registrar(ANFITRION, "publicacion_aprobada", "publicacion", PUBLICACION, null).notificacion();

        assertEquals(TipoEvento.PUBLICACION_APROBADA, guardada.getTipoEvento());
        assertEquals(TipoEntidad.PUBLICACION, guardada.getEntidadTipo());
    }

    // ---------- Decisiones de análisis ----------

    @Test
    @DisplayName("Decisión 1: un tipo de evento desconocido se rechaza")
    void rechazaTipoDesconocido() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> notificacionService.registrar(HUESPED, "PAGO_RECIBIDO", null, RESERVA, null));

        assertTrue(error.getMessage().contains("Tipo de evento no válido"));
        assertEquals(0, notificacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 2: si la entidad enviada no corresponde al evento se rechaza")
    void rechazaEntidadQueNoCorresponde() {
        assertThrows(SolicitudInvalidaException.class,
                () -> notificacionService.registrar(HUESPED, "RESERVA_CONFIRMADA", "PUBLICACION", RESERVA, null));

        assertEquals(0, notificacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 2: si la entidad viene vacía se deduce del evento")
    void entidadVaciaSeDeduce() {
        Notificacion guardada = notificacionService
                .registrar(HUESPED, "RESERVA_CONFIRMADA", "  ", RESERVA, null).notificacion();

        assertEquals(TipoEntidad.RESERVA, guardada.getEntidadTipo());
    }

    @Test
    @DisplayName("Decisión 3: un reintento del mismo evento no duplica la notificación")
    void reintentoNoDuplica() {
        RegistroNotificacion primero = notificacionService.registrar(HUESPED, "RESERVA_CONFIRMADA", null, RESERVA, null);
        RegistroNotificacion reintento = notificacionService.registrar(HUESPED, "RESERVA_CONFIRMADA", null, RESERVA,
                null);

        assertTrue(primero.nueva());
        assertFalse(reintento.nueva());
        assertEquals(primero.notificacion().getId(), reintento.notificacion().getId());
        assertEquals(1, notificacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 3: el mismo evento sobre otra entidad sí genera otra notificación")
    void otraEntidadSiGeneraNotificacion() {
        notificacionService.registrar(HUESPED, "RESERVA_CONFIRMADA", null, RESERVA, null);
        notificacionService.registrar(HUESPED, "RESERVA_CONFIRMADA", null, 301L, null);

        assertEquals(2, notificacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 4: sin mensaje se usa el mensaje por defecto del evento")
    void usaMensajePorDefecto() {
        Notificacion guardada = notificacionService
                .registrar(ANFITRION, "PUBLICACION_APROBADA", null, PUBLICACION, "").notificacion();

        assertEquals(TipoEvento.PUBLICACION_APROBADA.getMensajePorDefecto(), guardada.getMensaje());
    }

    @Test
    @DisplayName("Decisión 4: si envían mensaje se guarda sin espacios sobrantes")
    void guardaMensajeEnviado() {
        Notificacion guardada = notificacionService
                .registrar(HUESPED, "RESERVA_CONFIRMADA", null, RESERVA, "  Tu reserva fue confirmada  ")
                .notificacion();

        assertNotNull(guardada.getId());
        assertEquals("Tu reserva fue confirmada", guardada.getMensaje());
    }
}
