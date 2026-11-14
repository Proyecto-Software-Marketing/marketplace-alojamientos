package co.edu.unbosque.marketplace.disponibilidad.service;

import co.edu.unbosque.marketplace.disponibilidad.exception.ConflictoDisponibilidadException;
import co.edu.unbosque.marketplace.disponibilidad.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-18: bloqueo automático de periodos
@SpringBootTest
class BloqueoServiceTest {

    private static final Long PUBLICACION = 15L;
    private static final Long OTRA_PUBLICACION = 16L;

    @Autowired
    private BloqueoService bloqueoService;

    @Autowired
    private PeriodoDisponibleRepository periodoRepository;

    @Autowired
    private BloqueoReservaRepository bloqueoRepository;

    @BeforeEach
    void limpiarDatos() {
        bloqueoRepository.deleteAll();
        periodoRepository.deleteAll();
    }

    private LocalDate dia(int diasDesdeHoy) {
        return LocalDate.now().plusDays(diasDesdeHoy);
    }

    private void periodo(Long idPublicacion, LocalDate inicio, LocalDate fin) {
        periodoRepository.save(new PeriodoDisponible(idPublicacion, inicio, fin));
    }

    // ---------- CA-050 ----------

    @Test
    @DisplayName("CA-050 al confirmarse una reserva su periodo queda guardado como bloqueo")
    void bloqueaReservaConfirmada() {
        periodo(PUBLICACION, dia(1), dia(20));

        // Dado que se confirma una reserva, cuando se guarda
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        // Entonces su periodo queda bloqueado
        BloqueoReserva bloqueo = bloqueoRepository.findByIdReserva(500L).orElseThrow();
        assertEquals(PUBLICACION, bloqueo.getIdPublicacion());
        assertEquals(dia(5), bloqueo.getFechaLlegada());
        assertEquals(dia(8), bloqueo.getFechaSalida());
    }

    @Test
    @DisplayName("CA-050 las fechas bloqueadas dejan de estar disponibles para nuevas reservas")
    void fechasBloqueadasNoDisponibles() {
        periodo(PUBLICACION, dia(1), dia(20));
        assertTrue(bloqueoService.estaDisponible(PUBLICACION, dia(5), dia(8)));

        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        assertFalse(bloqueoService.estaDisponible(PUBLICACION, dia(5), dia(8)));
        assertFalse(bloqueoService.estaDisponible(PUBLICACION, dia(7), dia(10)));
        assertTrue(bloqueoService.estaDisponible(PUBLICACION, dia(10), dia(12)));
    }

    // ---------- CA-052 ----------

    @Test
    @DisplayName("CA-052 se rechaza una reserva que se cruza con un periodo bloqueado")
    void rechazaReservaCruzada() {
        // Dado que un periodo está bloqueado
        periodo(PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        // Cuando otro huésped intenta reservar fechas que se cruzan, entonces se rechaza
        assertThrows(ConflictoDisponibilidadException.class,
                () -> bloqueoService.bloquear(PUBLICACION, 501L, dia(7), dia(10)));
        assertEquals(1, bloqueoRepository.count());
    }

    @Test
    @DisplayName("CA-052 una reserva que empieza el día que sale otra sí se permite")
    void permiteReservasContiguas() {
        periodo(PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));
        bloqueoService.bloquear(PUBLICACION, 501L, dia(8), dia(10));

        assertEquals(2, bloqueoRepository.count());
    }

    @Test
    @DisplayName("CA-052 la consulta de disponibilidad excluye las publicaciones con fechas bloqueadas")
    void checkExcluyePublicacionesBloqueadas() {
        periodo(PUBLICACION, dia(1), dia(20));
        periodo(OTRA_PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        assertEquals(List.of(OTRA_PUBLICACION),
                bloqueoService.disponibles(List.of(PUBLICACION, OTRA_PUBLICACION), dia(6), dia(9)));
    }

    // ---------- Decisión 2: cobertura de periodos ----------

    @Test
    @DisplayName("Decisión 2: no se bloquean fechas fuera de los periodos disponibles")
    void rechazaFechasSinPeriodo() {
        periodo(PUBLICACION, dia(1), dia(10));

        assertThrows(ConflictoDisponibilidadException.class,
                () -> bloqueoService.bloquear(PUBLICACION, 500L, dia(8), dia(12)));
        assertEquals(0, bloqueoRepository.count());
    }

    @Test
    @DisplayName("Decisión 2: una reserva puede abarcar dos periodos contiguos")
    void permitePeriodosContiguos() {
        periodo(PUBLICACION, dia(1), dia(10));
        periodo(PUBLICACION, dia(10), dia(20));

        assertDoesNotThrow(() -> bloqueoService.bloquear(PUBLICACION, 500L, dia(8), dia(12)));
    }

    @Test
    @DisplayName("Decisión 2: no se bloquea si hay un hueco entre los periodos")
    void rechazaHuecoEntrePeriodos() {
        periodo(PUBLICACION, dia(1), dia(10));
        periodo(PUBLICACION, dia(12), dia(20));

        assertThrows(ConflictoDisponibilidadException.class,
                () -> bloqueoService.bloquear(PUBLICACION, 500L, dia(8), dia(14)));
    }

    // ---------- Decisión 3: idempotencia ----------

    @Test
    @DisplayName("Decisión 3: un reintento con los mismos datos no duplica el bloqueo")
    void reintentoIdempotente() {
        periodo(PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        assertDoesNotThrow(() -> bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8)));
        assertEquals(1, bloqueoRepository.count());
    }

    @Test
    @DisplayName("Decisión 3: la misma reserva con otras fechas se rechaza")
    void mismaReservaOtrasFechas() {
        periodo(PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        assertThrows(ConflictoDisponibilidadException.class,
                () -> bloqueoService.bloquear(PUBLICACION, 500L, dia(10), dia(12)));
    }

    // ---------- Decisión 4: liberar ----------

    @Test
    @DisplayName("Decisión 4: al liberar una reserva sus fechas vuelven a estar disponibles")
    void liberarDevuelveDisponibilidad() {
        periodo(PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(PUBLICACION, 500L, dia(5), dia(8));

        bloqueoService.liberar(500L);

        assertEquals(0, bloqueoRepository.count());
        assertTrue(bloqueoService.estaDisponible(PUBLICACION, dia(5), dia(8)));
    }

    @Test
    @DisplayName("Decisión 4: liberar una reserva sin bloqueo no falla")
    void liberarSinBloqueo() {
        assertDoesNotThrow(() -> bloqueoService.liberar(999L));
    }

    // ---------- Validaciones ----------

    @Test
    @DisplayName("Se rechaza un bloqueo con salida no posterior a la llegada")
    void rechazaFechasInvalidas() {
        assertThrows(FechasInvalidasException.class,
                () -> bloqueoService.bloquear(PUBLICACION, 500L, dia(8), dia(8)));
        List<Long> publicaciones = List.of(PUBLICACION);
        assertThrows(FechasInvalidasException.class,
                () -> bloqueoService.disponibles(publicaciones, dia(8), dia(5)));
    }

    @Test
    @DisplayName("Un bloqueo de otra publicación no afecta la disponibilidad")
    void bloqueoDeOtraPublicacion() {
        periodo(PUBLICACION, dia(1), dia(20));
        periodo(OTRA_PUBLICACION, dia(1), dia(20));
        bloqueoService.bloquear(OTRA_PUBLICACION, 500L, dia(5), dia(8));

        assertTrue(bloqueoService.estaDisponible(PUBLICACION, dia(5), dia(8)));
    }
}
