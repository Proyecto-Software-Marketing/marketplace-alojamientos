package co.edu.unbosque.marketplace.notificaciones.service;

import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEntidad;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEvento;
import co.edu.unbosque.marketplace.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la parte de Notificaciones en la HU-04: recuperación simulada de contraseña
@SpringBootTest
class RecuperacionNotificacionServiceTest {

    private static final Long HUESPED = 21L;
    private static final Long OTRO_USUARIO = 99L;
    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 15, 10, 0);
    private static final String MENSAJE = "Tu token para recuperar la contraseña es: abc. Vence en 30 minutos.";

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @BeforeEach
    void limpiarDatos() {
        notificacionRepository.deleteAll();
    }

    private Notificacion notificacion(Long idUsuario, TipoEvento tipo, Long idEntidad, int horasAtras) {
        return notificacionRepository.save(new Notificacion(idUsuario, tipo, idEntidad, MENSAJE,
                HOY.minusHours(horasAtras)));
    }

    @Test
    @DisplayName("CA-010 Identidad registra la notificación de recuperación con el token en el mensaje")
    void registraRecuperacion() {
        // Dado que Identidad generó la solicitud de recuperación 5 del huésped
        // Cuando avisa el evento con el token en el mensaje
        RegistroNotificacion registro = notificacionService.registrar(HUESPED, "RECUPERACION_CONTRASENA",
                "USUARIO", 5L, MENSAJE);

        // Entonces queda una notificación del usuario con el token
        assertTrue(registro.nueva());
        assertEquals(TipoEntidad.USUARIO, registro.notificacion().getEntidadTipo());
        assertEquals(MENSAJE, registro.notificacion().getMensaje());
    }

    @Test
    @DisplayName("Decisión 9: cada solicitud (otro entidadId) genera una notificación nueva con su propio token")
    void cadaSolicitudGeneraNotificacion() {
        notificacionService.registrar(HUESPED, "RECUPERACION_CONTRASENA", null, 5L, MENSAJE);
        RegistroNotificacion segunda = notificacionService.registrar(HUESPED, "RECUPERACION_CONTRASENA", null, 6L,
                "Tu token para recuperar la contraseña es: xyz. Vence en 30 minutos.");

        assertTrue(segunda.nueva());
        assertEquals(2, notificacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 10: devuelve la notificación de recuperación más reciente del usuario")
    void ultimaRecuperacion() {
        notificacion(HUESPED, TipoEvento.RECUPERACION_CONTRASENA, 5L, 3);
        Notificacion reciente = notificacion(HUESPED, TipoEvento.RECUPERACION_CONTRASENA, 6L, 1);
        notificacion(HUESPED, TipoEvento.RESERVA_CONFIRMADA, 300L, 0);
        notificacion(OTRO_USUARIO, TipoEvento.RECUPERACION_CONTRASENA, 7L, 0);

        Optional<Notificacion> resultado = notificacionService.ultimaRecuperacion(HUESPED);

        assertEquals(reciente.getId(), resultado.orElseThrow().getId());
    }

    @Test
    @DisplayName("Decisión 10: sin solicitudes de recuperación no devuelve nada")
    void sinRecuperacion() {
        notificacion(HUESPED, TipoEvento.RESERVA_CONFIRMADA, 300L, 0);

        assertTrue(notificacionService.ultimaRecuperacion(HUESPED).isEmpty());
    }
}
