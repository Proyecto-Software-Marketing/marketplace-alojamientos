package co.edu.unbosque.marketplace.notificaciones.service;

import co.edu.unbosque.marketplace.notificaciones.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEvento;
import co.edu.unbosque.marketplace.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-31: consultar notificaciones
@SpringBootTest
class ConsultaNotificacionesServiceTest {

    private static final Long HUESPED = 21L;
    private static final Long OTRO_USUARIO = 99L;
    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 15, 10, 0);

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @BeforeEach
    void limpiarDatos() {
        notificacionRepository.deleteAll();
    }

    // Guarda una notificación con la fecha indicada (hace "horas" horas)
    private Notificacion notificacion(Long idUsuario, Long idEntidad, int horasAtras) {
        return notificacionRepository.save(new Notificacion(idUsuario, TipoEvento.RESERVA_CONFIRMADA, idEntidad,
                "Tu reserva fue confirmada", HOY.minusHours(horasAtras)));
    }

    private List<Long> entidades(List<Notificacion> notificaciones) {
        return notificaciones.stream().map(Notificacion::getIdEntidad).toList();
    }

    // ---------- CA-085 ----------

    @Test
    @DisplayName("CA-085 las notificaciones salen de la más reciente a la más antigua")
    void ordenaDeLaMasReciente() {
        // Dado que tengo notificaciones registradas en distinto orden
        notificacion(HUESPED, 1L, 48);
        notificacion(HUESPED, 2L, 1);
        notificacion(HUESPED, 3L, 5);

        // Cuando las consulto
        List<Notificacion> lista = notificacionService.listar(HUESPED);

        // Entonces la más reciente va primero
        assertEquals(List.of(2L, 3L, 1L), entidades(lista));
    }

    @Test
    @DisplayName("CA-085 con la misma fecha va primero la registrada después")
    void mismaFechaPrimeroLaUltima() {
        notificacion(HUESPED, 1L, 2);
        notificacion(HUESPED, 2L, 2);

        assertEquals(List.of(2L, 1L), entidades(notificacionService.listar(HUESPED)));
    }

    @Test
    @DisplayName("CA-085 las no leídas se diferencian de las leídas")
    void diferenciaNoLeidas() {
        Notificacion leida = notificacion(HUESPED, 1L, 3);
        notificacion(HUESPED, 2L, 1);
        notificacionService.marcarLeida(HUESPED, leida.getId());

        List<Notificacion> lista = notificacionService.listar(HUESPED);

        assertFalse(lista.get(0).isLeida());
        assertTrue(lista.get(1).isLeida());
    }

    @Test
    @DisplayName("CA-085 sin notificaciones la lista está vacía")
    void sinNotificaciones() {
        assertTrue(notificacionService.listar(HUESPED).isEmpty());
    }

    // ---------- CA-086 ----------

    @Test
    @DisplayName("CA-086 al marcar una notificación no leída queda leída")
    void marcaComoLeida() {
        // Dado que tengo una notificación no leída
        Notificacion pendiente = notificacion(HUESPED, 1L, 1);

        // Cuando la marco como leída
        notificacionService.marcarLeida(HUESPED, pendiente.getId());

        // Entonces su estado cambia y se guarda
        assertTrue(notificacionRepository.findById(pendiente.getId()).orElseThrow().isLeida());
    }

    @Test
    @DisplayName("Decisión 1: marcar otra vez una notificación ya leída no da error")
    void marcarDosVecesNoFalla() {
        Notificacion pendiente = notificacion(HUESPED, 1L, 1);
        notificacionService.marcarLeida(HUESPED, pendiente.getId());

        assertTrue(notificacionService.marcarLeida(HUESPED, pendiente.getId()).isLeida());
    }

    @Test
    @DisplayName("CA-086 marcar una notificación no cambia las demás")
    void soloCambiaLaMarcada() {
        Notificacion primera = notificacion(HUESPED, 1L, 2);
        Notificacion segunda = notificacion(HUESPED, 2L, 1);

        notificacionService.marcarLeida(HUESPED, primera.getId());

        assertFalse(notificacionRepository.findById(segunda.getId()).orElseThrow().isLeida());
    }

    @Test
    @DisplayName("CA-086 marcar una notificación que no existe se rechaza")
    void noExiste() {
        assertThrows(NoEncontradoException.class, () -> notificacionService.marcarLeida(HUESPED, 12345L));
    }

    // ---------- CA-087 ----------

    @Test
    @DisplayName("CA-087 al consultar solo veo mis notificaciones")
    void soloLasMias() {
        // Dado que hay notificaciones mías y de otro usuario
        notificacion(HUESPED, 1L, 2);
        notificacion(OTRO_USUARIO, 2L, 1);
        notificacion(HUESPED, 3L, 3);

        // Cuando consulto las mías
        List<Notificacion> lista = notificacionService.listar(HUESPED);

        // Entonces no aparece la del otro usuario
        assertEquals(List.of(1L, 3L), entidades(lista));
        assertTrue(lista.stream().allMatch(n -> n.getIdUsuario().equals(HUESPED)));
    }

    @Test
    @DisplayName("CA-087 / Decisión 2: no puedo marcar la notificación de otro usuario")
    void noMarcaLaDeOtro() {
        Notificacion ajena = notificacion(OTRO_USUARIO, 1L, 1);

        assertThrows(NoEncontradoException.class, () -> notificacionService.marcarLeida(HUESPED, ajena.getId()));
        assertFalse(notificacionRepository.findById(ajena.getId()).orElseThrow().isLeida());
    }
}
