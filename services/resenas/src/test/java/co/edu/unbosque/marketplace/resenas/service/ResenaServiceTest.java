package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.exception.ConflictoResenaException;
import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// Pruebas de la HU-32: calificar y reseñar una estancia
@SpringBootTest
class ResenaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 10, 30);
    private static final Long HUESPED = 21L;
    private static final Long OTRO_HUESPED = 99L;
    private static final Long PUBLICACION = 15L;

    @Autowired
    private ResenaService resenaService;

    @Autowired
    private ResenaRepository resenaRepository;

    // Reloj simulado para comprobar la fecha de la reseña
    @MockitoBean
    private Clock reloj;

    @BeforeEach
    void preparar() {
        resenaRepository.deleteAll();
        when(reloj.getZone()).thenReturn(BOGOTA);
        when(reloj.instant()).thenReturn(AHORA.atZone(BOGOTA).toInstant());
    }

    // Reserva tal como la devuelve Reservas
    private ReservaDTO reserva(Long id, Long huespedId, String estado) {
        return new ReservaDTO(id, PUBLICACION, huespedId, estado);
    }

    // ---------- CA-088 ----------

    @Test
    @DisplayName("CA-088 con una reserva COMPLETADA propia la reseña queda publicada en la publicación")
    void registraResena() {
        // Dado que tengo una reserva COMPLETADA propia sin reseña
        ReservaDTO completada = reserva(300L, HUESPED, "COMPLETADA");

        // Cuando la califico con 5 y escribo la reseña
        Resena resena = resenaService.registrar(HUESPED, completada, 5, "  Excelente lugar, muy limpio  ");

        // Entonces queda publicada en la publicación de la reserva
        assertNotNull(resena.getId());
        assertEquals(300L, resena.getReservaId());
        assertEquals(PUBLICACION, resena.getPublicacionId());
        assertEquals(HUESPED, resena.getHuespedId());
        assertEquals(5, resena.getCalificacion());
        assertEquals("Excelente lugar, muy limpio", resena.getContenido());
        assertTrue(resena.isVisible());
        assertEquals(AHORA, resena.getFecha());
        assertEquals(1, resenaRepository.count());
    }

    @ParameterizedTest(name = "calificación {0}")
    @ValueSource(ints = {1, 5})
    @DisplayName("CA-088 se aceptan los extremos del rango 1 y 5")
    void aceptaExtremos(int calificacion) {
        Resena resena = resenaService.registrar(HUESPED, reserva(300L, HUESPED, "COMPLETADA"), calificacion,
                "Buena estancia");

        assertEquals(calificacion, resena.getCalificacion());
    }

    @Test
    @DisplayName("Decisión 1: se aceptan reseñas de exactamente 1000 caracteres")
    void aceptaLongitudMaxima() {
        Resena resena = resenaService.registrar(HUESPED, reserva(300L, HUESPED, "COMPLETADA"), 4,
                "a".repeat(1000));

        assertEquals(1000, resena.getContenido().length());
    }

    // ---------- CA-089 ----------

    @ParameterizedTest(name = "estado {0}")
    @ValueSource(strings = {"CONFIRMADA", "CANCELADA", "EN_REVISION_MANUAL"})
    @DisplayName("CA-089 si la reserva no está COMPLETADA no se permite reseñarla (409)")
    void rechazaReservaNoCompletada(String estado) {
        // Dado que mi reserva no está COMPLETADA
        ReservaDTO noCompletada = reserva(300L, HUESPED, estado);

        // Cuando intento reseñarla, entonces no se permite
        assertThrows(ConflictoResenaException.class,
                () -> resenaService.registrar(HUESPED, noCompletada, 4, "Todavía no termina"));
        assertEquals(0, resenaRepository.count());
    }

    // ---------- CA-090 ----------

    @Test
    @DisplayName("CA-090 si ya reseñé la reserva no puedo reseñarla de nuevo (409)")
    void rechazaSegundaResena() {
        // Dado que ya reseñé la reserva
        ReservaDTO completada = reserva(300L, HUESPED, "COMPLETADA");
        resenaService.registrar(HUESPED, completada, 5, "Primera reseña");

        // Cuando intento reseñarla de nuevo, entonces no se permite y queda solo la primera
        assertThrows(ConflictoResenaException.class,
                () -> resenaService.registrar(HUESPED, completada, 1, "Segunda reseña"));
        assertEquals(1, resenaRepository.count());
    }

    @Test
    @DisplayName("CA-090 reseñar otra reserva completada propia sí se permite")
    void permiteOtraReserva() {
        resenaService.registrar(HUESPED, reserva(300L, HUESPED, "COMPLETADA"), 5, "Primera estancia");
        resenaService.registrar(HUESPED, reserva(301L, HUESPED, "COMPLETADA"), 3, "Segunda estancia");

        assertEquals(2, resenaRepository.count());
    }

    // ---------- CA-091 ----------

    @ParameterizedTest(name = "calificación {0}")
    @ValueSource(ints = {0, 6, -1})
    @DisplayName("CA-091 una calificación fuera del rango de 1 a 5 se rechaza")
    void rechazaCalificacionFueraDeRango(int calificacion) {
        ReservaDTO completada = reserva(300L, HUESPED, "COMPLETADA");

        assertThrows(SolicitudInvalidaException.class,
                () -> resenaService.registrar(HUESPED, completada, calificacion, "Fuera de rango"));
        assertEquals(0, resenaRepository.count());
    }

    @Test
    @DisplayName("CA-091 sin calificación se rechaza")
    void rechazaSinCalificacion() {
        ReservaDTO completada = reserva(300L, HUESPED, "COMPLETADA");

        assertThrows(SolicitudInvalidaException.class,
                () -> resenaService.registrar(HUESPED, completada, null, "Sin calificación"));
    }

    // ---------- Decisión 1: contenido ----------

    @Test
    @DisplayName("Decisión 1: el contenido vacío se rechaza")
    void rechazaContenidoVacio() {
        ReservaDTO completada = reserva(300L, HUESPED, "COMPLETADA");

        assertThrows(SolicitudInvalidaException.class,
                () -> resenaService.registrar(HUESPED, completada, 4, "   "));
        assertThrows(SolicitudInvalidaException.class,
                () -> resenaService.registrar(HUESPED, completada, 4, null));
    }

    @Test
    @DisplayName("Decisión 1: el contenido de más de 1000 caracteres se rechaza")
    void rechazaContenidoLargo() {
        ReservaDTO completada = reserva(300L, HUESPED, "COMPLETADA");

        assertThrows(SolicitudInvalidaException.class,
                () -> resenaService.registrar(HUESPED, completada, 4, "a".repeat(1001)));
    }

    // ---------- Decisión 2: solo reservas propias ----------

    @Test
    @DisplayName("Decisión 2: la reserva de otro huésped responde como si no existiera (404)")
    void rechazaReservaDeOtroHuesped() {
        ReservaDTO ajena = reserva(300L, OTRO_HUESPED, "COMPLETADA");

        assertThrows(NoEncontradoException.class,
                () -> resenaService.registrar(HUESPED, ajena, 5, "No es mía"));
        assertEquals(0, resenaRepository.count());
    }
}
