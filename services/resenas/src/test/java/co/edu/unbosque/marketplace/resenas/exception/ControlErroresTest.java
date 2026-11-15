package co.edu.unbosque.marketplace.resenas.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Casos de ControlErrores que no se alcanzan fácilmente por HTTP
class ControlErroresTest {

    private final ControlErrores controlErrores = new ControlErrores();

    @Test
    @DisplayName("Decisión 4: si dos solicitudes chocan con la restricción única se responde 409")
    void duplicadoPorRestriccionUnica() {
        ResponseEntity<Map<String, List<String>>> respuesta =
                controlErrores.manejarDuplicado(new DataIntegrityViolationException("uk_resena_reserva"));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(List.of("Esta reserva ya tiene una reseña"), respuesta.getBody().get("mensajes"));
    }

    @Test
    @DisplayName("CA-091: una regla verificada en el servicio responde 400 con su mensaje")
    void solicitudInvalida() {
        ResponseEntity<Map<String, List<String>>> respuesta = controlErrores.manejarSolicitud(
                new SolicitudInvalidaException("La calificación debe ser un número entero entre 1 y 5"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(List.of("La calificación debe ser un número entero entre 1 y 5"),
                respuesta.getBody().get("mensajes"));
    }
}
