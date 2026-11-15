package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-33: consultar reseñas y calificación promedio
@SpringBootTest
class ConsultaResenasServiceTest {

    private static final Long PUBLICACION = 15L;
    private static final Long OTRA_PUBLICACION = 16L;
    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 15, 10, 0);

    @Autowired
    private ConsultaResenasService consultaResenasService;

    @Autowired
    private ResenaRepository resenaRepository;

    private long siguienteReserva = 300L;

    @BeforeEach
    void limpiarDatos() {
        resenaRepository.deleteAll();
    }

    // Guarda una reseña visible de la publicación, registrada hace "horasAtras" horas
    private Resena resena(Long publicacionId, int calificacion, int horasAtras) {
        return resenaRepository.save(new Resena(siguienteReserva++, publicacionId, 21L, calificacion,
                "Reseña con " + calificacion, HOY.minusHours(horasAtras)));
    }

    // Guarda una reseña que un administrador ocultó
    private Resena oculta(Long publicacionId, int calificacion, int horasAtras) {
        Resena resena = new Resena(siguienteReserva++, publicacionId, 21L, calificacion, "Oculta",
                HOY.minusHours(horasAtras));
        resena.ocultar();
        return resenaRepository.save(resena);
    }

    private List<Integer> calificaciones(ResenasPublicacion resultado) {
        return resultado.resenas().stream().map(Resena::getCalificacion).toList();
    }

    // ---------- CA-092 ----------

    @Test
    @DisplayName("CA-092 veo las reseñas visibles de la publicación y su calificación promedio")
    void listaResenasYPromedio() {
        // Dado que la publicación tiene reseñas visibles
        resena(PUBLICACION, 5, 3);
        resena(PUBLICACION, 4, 2);
        resena(PUBLICACION, 4, 1);

        // Cuando veo su detalle
        ResenasPublicacion resultado = consultaResenasService.listarVisibles(PUBLICACION);

        // Entonces veo las reseñas y el promedio (13 / 3 = 4.33 → 4.3)
        assertEquals(3, resultado.resenas().size());
        assertEquals(4.3, resultado.calificacionPromedio());
    }

    @Test
    @DisplayName("Decisión 4: las reseñas salen de la más reciente a la más antigua")
    void ordenaDeLaMasReciente() {
        resena(PUBLICACION, 1, 48);
        resena(PUBLICACION, 2, 1);
        resena(PUBLICACION, 3, 5);

        assertEquals(List.of(2, 3, 1), calificaciones(consultaResenasService.listarVisibles(PUBLICACION)));
    }

    @Test
    @DisplayName("Decisión 4: con la misma fecha va primero la registrada después")
    void mismaFechaPrimeroLaUltima() {
        resena(PUBLICACION, 1, 2);
        resena(PUBLICACION, 2, 2);

        assertEquals(List.of(2, 1), calificaciones(consultaResenasService.listarVisibles(PUBLICACION)));
    }

    @Test
    @DisplayName("CA-092 solo aparecen las reseñas de esa publicación")
    void soloDeLaPublicacion() {
        resena(PUBLICACION, 5, 1);
        resena(OTRA_PUBLICACION, 1, 1);

        ResenasPublicacion resultado = consultaResenasService.listarVisibles(PUBLICACION);

        assertEquals(List.of(5), calificaciones(resultado));
        assertEquals(5.0, resultado.calificacionPromedio());
    }

    @Test
    @DisplayName("Decisión 3: el promedio se redondea a un decimal hacia arriba desde ,05")
    void redondeaPromedio() {
        // (3 + 4) / 2 = 3.5; (4 + 4 + 3) / 3 = 3.67 → 3.7
        resena(PUBLICACION, 3, 2);
        resena(PUBLICACION, 4, 1);
        assertEquals(3.5, consultaResenasService.listarVisibles(PUBLICACION).calificacionPromedio());

        resena(PUBLICACION, 4, 0);
        assertEquals(3.7, consultaResenasService.listarVisibles(PUBLICACION).calificacionPromedio());
    }

    // ---------- CA-093 ----------

    @Test
    @DisplayName("CA-093 la reseña oculta no aparece ni cuenta en el promedio")
    void excluyeOcultas() {
        // Dado que un administrador ocultó la reseña de 1 estrella
        resena(PUBLICACION, 5, 2);
        resena(PUBLICACION, 4, 1);
        oculta(PUBLICACION, 1, 0);

        // Cuando veo las reseñas de la publicación
        ResenasPublicacion resultado = consultaResenasService.listarVisibles(PUBLICACION);

        // Entonces no aparece y el promedio es (5 + 4) / 2 = 4.5, no 3.3
        assertEquals(List.of(4, 5), calificaciones(resultado));
        assertEquals(4.5, resultado.calificacionPromedio());
    }

    @Test
    @DisplayName("CA-093 y decisión 3: si todas están ocultas no hay reseñas ni promedio")
    void todasOcultas() {
        oculta(PUBLICACION, 2, 1);

        ResenasPublicacion resultado = consultaResenasService.listarVisibles(PUBLICACION);

        assertTrue(resultado.resenas().isEmpty());
        assertNull(resultado.calificacionPromedio());
    }

    // ---------- Decisiones 3 y 5 ----------

    @Test
    @DisplayName("Decisiones 3 y 5: una publicación sin reseñas responde la lista vacía y promedio null")
    void sinResenas() {
        ResenasPublicacion resultado = consultaResenasService.listarVisibles(999L);

        assertTrue(resultado.resenas().isEmpty());
        assertNull(resultado.calificacionPromedio());
    }
}
