package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// Pruebas de la HU-36: ocultar reseñas
@SpringBootTest
class ModeracionResenasServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 16, 0);
    private static final Long ADMIN = 1L;
    private static final Long OTRO_ADMIN = 2L;
    private static final Long PUBLICACION = 15L;

    @Autowired
    private ModeracionResenasService moderacionResenasService;

    @Autowired
    private ConsultaResenasService consultaResenasService;

    @Autowired
    private ResenaRepository resenaRepository;

    // Reloj simulado para comprobar cuándo se ocultó
    @MockitoBean
    private Clock reloj;

    @BeforeEach
    void preparar() {
        resenaRepository.deleteAll();
        when(reloj.getZone()).thenReturn(BOGOTA);
        when(reloj.instant()).thenReturn(AHORA.atZone(BOGOTA).toInstant());
    }

    private Resena resena(Long reservaId, int calificacion) {
        return resenaRepository.save(new Resena(reservaId, PUBLICACION, 21L, calificacion, "Reseña",
                AHORA.minusDays(1)));
    }

    // ---------- CA-100 ----------

    @Test
    @DisplayName("CA-100 la reseña oculta deja de mostrarse en la publicación y no cuenta en el promedio")
    void ocultaYDejaDeContar() {
        // Dado que una reseña incumple las políticas
        resena(300L, 5);
        Resena ofensiva = resena(301L, 1);
        assertEquals(3.0, consultaResenasService.listarVisibles(PUBLICACION).calificacionPromedio());

        // Cuando la oculto
        ResultadoOcultamiento resultado = moderacionResenasService.ocultar(ADMIN, ofensiva.getId(),
                "Lenguaje ofensivo");

        // Entonces deja de mostrarse y no cuenta en el promedio
        assertTrue(resultado.recienOcultada());
        assertFalse(resultado.resena().isVisible());
        ResenasPublicacion publicacion = consultaResenasService.listarVisibles(PUBLICACION);
        assertEquals(1, publicacion.resenas().size());
        assertEquals(5.0, publicacion.calificacionPromedio());
    }

    // ---------- CA-101 ----------

    @Test
    @DisplayName("CA-101 queda registrado quién ocultó la reseña, cuándo y por qué")
    void registraQuienCuandoYPorQue() {
        Resena ofensiva = resena(301L, 1);

        moderacionResenasService.ocultar(ADMIN, ofensiva.getId(), "  Lenguaje ofensivo  ");

        Resena guardada = resenaRepository.findById(ofensiva.getId()).orElseThrow();
        assertFalse(guardada.isVisible());
        assertEquals(ADMIN, guardada.getOcultadaPor());
        assertEquals(AHORA, guardada.getOcultadaEn());
        assertEquals("Lenguaje ofensivo", guardada.getMotivoOcultamiento());
    }

    @Test
    @DisplayName("CA-101 una reseña visible no tiene datos de ocultamiento")
    void visibleSinDatosDeOcultamiento() {
        Resena visible = resena(300L, 4);

        assertNull(visible.getOcultadaPor());
        assertNull(visible.getOcultadaEn());
        assertNull(visible.getMotivoOcultamiento());
    }

    // ---------- Decisión 4 ----------

    @Test
    @DisplayName("Decisión 4: ocultar de nuevo no cambia nada y conserva el registro de la primera vez")
    void ocultarDosVecesEsIdempotente() {
        Resena ofensiva = resena(301L, 1);
        moderacionResenasService.ocultar(ADMIN, ofensiva.getId(), "Lenguaje ofensivo");

        ResultadoOcultamiento segunda = moderacionResenasService.ocultar(OTRO_ADMIN, ofensiva.getId(), "Spam");

        assertFalse(segunda.recienOcultada());
        Resena guardada = resenaRepository.findById(ofensiva.getId()).orElseThrow();
        assertEquals(ADMIN, guardada.getOcultadaPor());
        assertEquals("Lenguaje ofensivo", guardada.getMotivoOcultamiento());
    }

    // ---------- Decisiones 2 y 3 ----------

    @ParameterizedTest(name = "motivo [{0}]")
    @NullSource
    @ValueSource(strings = {"", "  ", "ab", "  ab  "})
    @DisplayName("Decisión 2: el motivo es obligatorio y de al menos 3 caracteres")
    void rechazaMotivoCorto(String motivo) {
        Resena resena = resena(300L, 2);

        assertThrows(SolicitudInvalidaException.class,
                () -> moderacionResenasService.ocultar(ADMIN, resena.getId(), motivo));
        assertTrue(resenaRepository.findById(resena.getId()).orElseThrow().isVisible());
    }

    @Test
    @DisplayName("Decisión 2: el motivo no puede superar 500 caracteres")
    void rechazaMotivoLargo() {
        Resena resena = resena(300L, 2);

        assertThrows(SolicitudInvalidaException.class,
                () -> moderacionResenasService.ocultar(ADMIN, resena.getId(), "a".repeat(501)));
    }

    @Test
    @DisplayName("Decisión 2: se acepta un motivo de exactamente 500 caracteres")
    void aceptaMotivoDe500() {
        Resena resena = resena(300L, 2);

        ResultadoOcultamiento resultado = moderacionResenasService.ocultar(ADMIN, resena.getId(), "a".repeat(500));

        assertEquals(500, resultado.resena().getMotivoOcultamiento().length());
    }

    @Test
    @DisplayName("Decisión 3: si la reseña no existe se responde 404")
    void resenaNoExiste() {
        assertThrows(NoEncontradoException.class,
                () -> moderacionResenasService.ocultar(ADMIN, 9999L, "Lenguaje ofensivo"));
    }
}
