package co.edu.unbosque.marketplace.disponibilidad.service;

import co.edu.unbosque.marketplace.disponibilidad.dto.TramoCalendarioDTO;
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

import static co.edu.unbosque.marketplace.disponibilidad.model.EstadoCalendario.BLOQUEADO;
import static co.edu.unbosque.marketplace.disponibilidad.model.EstadoCalendario.DISPONIBLE;
import static co.edu.unbosque.marketplace.disponibilidad.model.EstadoCalendario.RESERVADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-17: calendario de disponibilidad
@SpringBootTest
class CalendarioServiceTest {

    private static final Long PUBLICACION = 15L;
    private static final String ACTIVA = "ACTIVA";

    @Autowired
    private CalendarioService calendarioService;

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

    private Long periodo(LocalDate inicio, LocalDate fin) {
        return periodoRepository.save(new PeriodoDisponible(PUBLICACION, inicio, fin)).getId();
    }

    private void reserva(Long idPublicacion, Long idReserva, LocalDate llegada, LocalDate salida) {
        bloqueoRepository.save(new BloqueoReserva(idPublicacion, idReserva, llegada, salida));
    }

    // ---------- CA-048 ----------

    @Test
    @DisplayName("CA-048 un periodo sin reservas se muestra completo como disponible")
    void periodoSinReservas() {
        Long id = periodo(dia(1), dia(10));

        assertEquals(List.of(new TramoCalendarioDTO(id, dia(1), dia(10), DISPONIBLE)),
                calendarioService.construir(PUBLICACION, ACTIVA));
    }

    @Test
    @DisplayName("CA-048 una reserva en medio separa disponible, reservado y disponible")
    void reservaEnMedio() {
        // Dado que una publicación tiene periodos registrados y una reserva
        Long id = periodo(dia(1), dia(20));
        reserva(PUBLICACION, 500L, dia(5), dia(8));

        // Cuando consulto su calendario
        List<TramoCalendarioDTO> tramos = calendarioService.construir(PUBLICACION, ACTIVA);

        // Entonces veo los estados diferenciados
        assertEquals(List.of(
                new TramoCalendarioDTO(id, dia(1), dia(5), DISPONIBLE),
                new TramoCalendarioDTO(id, dia(5), dia(8), RESERVADO),
                new TramoCalendarioDTO(id, dia(8), dia(20), DISPONIBLE)), tramos);
    }

    @Test
    @DisplayName("CA-048 reservas en los bordes del periodo no generan tramos vacíos")
    void reservasEnLosBordes() {
        Long id = periodo(dia(1), dia(20));
        reserva(PUBLICACION, 500L, dia(1), dia(4));
        reserva(PUBLICACION, 501L, dia(15), dia(20));

        assertEquals(List.of(
                new TramoCalendarioDTO(id, dia(1), dia(4), RESERVADO),
                new TramoCalendarioDTO(id, dia(4), dia(15), DISPONIBLE),
                new TramoCalendarioDTO(id, dia(15), dia(20), RESERVADO)),
                calendarioService.construir(PUBLICACION, ACTIVA));
    }

    @Test
    @DisplayName("CA-048 una publicación bloqueada por un administrador se muestra bloqueada")
    void publicacionBloqueada() {
        Long id = periodo(dia(1), dia(20));
        reserva(PUBLICACION, 500L, dia(5), dia(8));

        assertEquals(List.of(new TramoCalendarioDTO(id, dia(1), dia(20), BLOQUEADO)),
                calendarioService.construir(PUBLICACION, "BLOQUEADA"));
    }

    @Test
    @DisplayName("CA-048 los periodos salen ordenados por fecha aunque se registren en otro orden")
    void periodosOrdenados() {
        Long segundo = periodo(dia(20), dia(30));
        Long primero = periodo(dia(1), dia(10));

        assertEquals(List.of(
                new TramoCalendarioDTO(primero, dia(1), dia(10), DISPONIBLE),
                new TramoCalendarioDTO(segundo, dia(20), dia(30), DISPONIBLE)),
                calendarioService.construir(PUBLICACION, ACTIVA));
    }

    // ---------- CA-049 ----------

    @Test
    @DisplayName("CA-049 al confirmarse una reserva el calendario la muestra como reservada")
    void reservaConfirmadaApareceReservada() {
        Long id = periodo(dia(1), dia(10));
        assertEquals(List.of(new TramoCalendarioDTO(id, dia(1), dia(10), DISPONIBLE)),
                calendarioService.construir(PUBLICACION, ACTIVA));

        // Dado que se confirma una reserva (en el Sprint 2 la registrará la HU-18)
        reserva(PUBLICACION, 500L, dia(3), dia(6));

        // Cuando vuelvo a consultar el calendario, entonces el periodo aparece como reservado
        assertTrue(calendarioService.construir(PUBLICACION, ACTIVA)
                .contains(new TramoCalendarioDTO(id, dia(3), dia(6), RESERVADO)));
    }

    @Test
    @DisplayName("Una reserva que cruza dos periodos contiguos se muestra en ambos")
    void reservaQueCruzaDosPeriodos() {
        Long primero = periodo(dia(1), dia(10));
        Long segundo = periodo(dia(10), dia(20));
        reserva(PUBLICACION, 500L, dia(8), dia(12));

        assertEquals(List.of(
                new TramoCalendarioDTO(primero, dia(1), dia(8), DISPONIBLE),
                new TramoCalendarioDTO(primero, dia(8), dia(10), RESERVADO),
                new TramoCalendarioDTO(segundo, dia(10), dia(12), RESERVADO),
                new TramoCalendarioDTO(segundo, dia(12), dia(20), DISPONIBLE)),
                calendarioService.construir(PUBLICACION, ACTIVA));
    }

    @Test
    @DisplayName("Las reservas de otra publicación no afectan el calendario")
    void reservaDeOtraPublicacion() {
        Long id = periodo(dia(1), dia(10));
        reserva(99L, 500L, dia(3), dia(6));

        assertEquals(List.of(new TramoCalendarioDTO(id, dia(1), dia(10), DISPONIBLE)),
                calendarioService.construir(PUBLICACION, ACTIVA));
    }

    // ---------- Decisión 3: desde hoy ----------

    @Test
    @DisplayName("Decisión 3: un periodo que ya empezó se muestra desde hoy")
    void periodoIniciadoDesdeHoy() {
        Long id = periodo(dia(-5), dia(10));

        assertEquals(List.of(new TramoCalendarioDTO(id, dia(0), dia(10), DISPONIBLE)),
                calendarioService.construir(PUBLICACION, ACTIVA));
    }

    @Test
    @DisplayName("Decisión 3: los periodos que ya terminaron no aparecen")
    void periodosPasadosNoAparecen() {
        periodo(dia(-10), dia(-2));
        periodo(dia(-5), dia(0));

        assertTrue(calendarioService.construir(PUBLICACION, ACTIVA).isEmpty());
    }
}