package co.edu.unbosque.marketplace.disponibilidad.service;

import co.edu.unbosque.marketplace.disponibilidad.exception.ConflictoDisponibilidadException;
import co.edu.unbosque.marketplace.disponibilidad.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.disponibilidad.exception.NoEncontradoException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-16: gestionar periodos disponibles
@SpringBootTest
class PeriodoServiceTest {

    private static final Long PUBLICACION = 15L;
    private static final Long OTRA_PUBLICACION = 99L;

    @Autowired
    private PeriodoService periodoService;

    @Autowired
    private PeriodoDisponibleRepository periodoRepository;

    @Autowired
    private BloqueoReservaRepository bloqueoRepository;

    @BeforeEach
    void limpiarDatos() {
        bloqueoRepository.deleteAll();
        periodoRepository.deleteAll();
    }

    // Fecha relativa a hoy, para que las pruebas funcionen cualquier día
    private LocalDate dia(int diasDesdeHoy) {
        return LocalDate.now().plusDays(diasDesdeHoy);
    }

    // Simula una reserva confirmada (en el Sprint 2 la creará la HU-18)
    private void reservaConfirmada(Long idReserva, LocalDate llegada, LocalDate salida) {
        bloqueoRepository.save(new BloqueoReserva(PUBLICACION, idReserva, llegada, salida));
    }

    // ---------- CA-045 ----------

    @Test
    @DisplayName("CA-045 un periodo con fechas válidas queda guardado")
    void registraPeriodoValido() {
        // Dado que tengo una publicación
        // Cuando registro un periodo con fechas válidas
        PeriodoDisponible periodo = periodoService.registrar(PUBLICACION, dia(1), dia(10));

        // Entonces queda guardado
        assertNotNull(periodo.getId());
        assertEquals(PUBLICACION, periodo.getIdPublicacion());
        assertEquals(dia(1), periodo.getFechaInicio());
        assertEquals(dia(10), periodo.getFechaFin());
        assertNotNull(periodo.getFechaCreacion());
        assertNull(periodo.getFechaActualizacion());
        assertEquals(1, periodoRepository.count());
    }

    // ---------- CA-046 ----------

    @Test
    @DisplayName("CA-046 se rechaza un periodo con salida igual a la llegada")
    void rechazaSalidaIgualALlegada() {
        assertThrows(FechasInvalidasException.class,
                () -> periodoService.registrar(PUBLICACION, dia(5), dia(5)));
        assertEquals(0, periodoRepository.count());
    }

    @Test
    @DisplayName("CA-046 se rechaza un periodo con salida antes de la llegada")
    void rechazaSalidaAntesDeLlegada() {
        assertThrows(FechasInvalidasException.class,
                () -> periodoService.registrar(PUBLICACION, dia(10), dia(5)));
        assertEquals(0, periodoRepository.count());
    }

    @Test
    @DisplayName("CA-046 al editar también se rechaza una salida que no es posterior a la llegada")
    void rechazaFechasInvalidasAlEditar() {
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(10)).getId();

        assertThrows(FechasInvalidasException.class,
                () -> periodoService.editar(PUBLICACION, id, dia(8), dia(3)));
    }

    // ---------- Decisiones de análisis de la HU-16 ----------

    @Test
    @DisplayName("Decisión 3: no se registra un periodo que empieza en el pasado")
    void rechazaPeriodoEnElPasado() {
        assertThrows(FechasInvalidasException.class,
                () -> periodoService.registrar(PUBLICACION, dia(-3), dia(5)));
    }

    @Test
    @DisplayName("Decisión 2: no se permiten periodos solapados en la misma publicación")
    void rechazaPeriodoSolapado() {
        periodoService.registrar(PUBLICACION, dia(1), dia(10));

        assertThrows(ConflictoDisponibilidadException.class,
                () -> periodoService.registrar(PUBLICACION, dia(5), dia(15)));
        assertEquals(1, periodoRepository.count());
    }

    @Test
    @DisplayName("Decisión 2: los periodos contiguos sí se permiten")
    void permitePeriodosContiguos() {
        periodoService.registrar(PUBLICACION, dia(1), dia(10));
        periodoService.registrar(PUBLICACION, dia(10), dia(15));

        assertEquals(2, periodoRepository.count());
    }

    // ---------- CA-047 ----------

    @Test
    @DisplayName("CA-047 no se puede eliminar un periodo con reservas confirmadas")
    void noEliminaConReservas() {
        // Dado que un periodo tiene reservas confirmadas
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(20)).getId();
        reservaConfirmada(500L, dia(5), dia(8));

        // Cuando intento eliminarlo, entonces el sistema no lo permite
        assertThrows(ConflictoDisponibilidadException.class,
                () -> periodoService.eliminar(PUBLICACION, id));
        assertTrue(periodoRepository.existsById(id));
    }

    @Test
    @DisplayName("CA-047 no se puede recortar un periodo dejando fuera una reserva confirmada")
    void noRecortaConReservas() {
        // Dado que un periodo tiene reservas confirmadas
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(20)).getId();
        reservaConfirmada(500L, dia(5), dia(8));

        // Cuando intento recortarlo, entonces el sistema no lo permite
        assertThrows(ConflictoDisponibilidadException.class,
                () -> periodoService.editar(PUBLICACION, id, dia(1), dia(6)));

        // y el periodo conserva sus fechas originales
        PeriodoDisponible guardado = periodoRepository.findById(id).orElseThrow();
        assertEquals(dia(20), guardado.getFechaFin());
    }

    @Test
    @DisplayName("CA-047 sí se puede ampliar un periodo con reservas confirmadas")
    void permiteAmpliarConReservas() {
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(20)).getId();
        reservaConfirmada(500L, dia(5), dia(8));

        PeriodoDisponible editado = periodoService.editar(PUBLICACION, id, dia(1), dia(25));

        assertEquals(dia(25), editado.getFechaFin());
        assertNotNull(editado.getFechaActualizacion());
    }

    @Test
    @DisplayName("CA-047 se puede recortar si las reservas siguen quedando dentro")
    void permiteRecortarSinAfectarReservas() {
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(20)).getId();
        reservaConfirmada(500L, dia(5), dia(8));

        PeriodoDisponible editado = periodoService.editar(PUBLICACION, id, dia(3), dia(20));

        assertEquals(dia(3), editado.getFechaInicio());
    }

    @Test
    @DisplayName("Se puede eliminar un periodo sin reservas")
    void eliminaSinReservas() {
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(10)).getId();

        periodoService.eliminar(PUBLICACION, id);

        assertEquals(0, periodoRepository.count());
    }

    // ---------- Casos borde ----------

    @Test
    @DisplayName("Un periodo que ya empezó se puede ampliar sin mover su inicio")
    void periodoIniciadoSePuedeAmpliar() {
        // Se guarda directo en el repositorio porque el servicio no deja crear periodos en el pasado
        Long id = periodoRepository.save(new PeriodoDisponible(PUBLICACION, dia(-5), dia(10))).getId();

        PeriodoDisponible editado = periodoService.editar(PUBLICACION, id, dia(-5), dia(20));

        assertEquals(dia(20), editado.getFechaFin());
    }

    @Test
    @DisplayName("No se puede mover el inicio de un periodo a una fecha pasada")
    void noMueveInicioAlPasado() {
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(10)).getId();

        assertThrows(FechasInvalidasException.class,
                () -> periodoService.editar(PUBLICACION, id, dia(-2), dia(10)));
    }

    @Test
    @DisplayName("No se encuentra un periodo si se busca desde otra publicación")
    void noEncuentraPeriodoDeOtraPublicacion() {
        Long id = periodoService.registrar(PUBLICACION, dia(1), dia(10)).getId();

        assertThrows(NoEncontradoException.class,
                () -> periodoService.editar(OTRA_PUBLICACION, id, dia(1), dia(12)));
    }

    @Test
    @DisplayName("No se puede eliminar un periodo que no existe")
    void noEliminaPeriodoInexistente() {
        assertThrows(NoEncontradoException.class,
                () -> periodoService.eliminar(PUBLICACION, 12345L));
    }
}