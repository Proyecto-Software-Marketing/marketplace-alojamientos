package co.edu.unbosque.marketplace.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import co.edu.unbosque.marketplace.reportes.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reportes.exception.ControlErrores;
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;


class ErroresTest {

    private final ControlErrores controlErrores = new ControlErrores();

    @Test
    void sinSesionResponde401() {
        ResponseEntity<Map<String, List<String>>> respuesta = controlErrores.errorAutenticacion(
                new AutenticacionException("Debe iniciar sesión"));

        assertEquals(401, respuesta.getStatusCode().value());
        assertEquals(List.of("Debe iniciar sesión"), respuesta.getBody().get("mensajes"));
    }

    @Test
    void sinPermisoResponde403() {
        ResponseEntity<Map<String, List<String>>> respuesta = controlErrores.errorPermiso(
                new PermisoException("Solo el administrador puede ver el dashboard"));

        assertEquals(403, respuesta.getStatusCode().value());
        assertEquals(List.of("Solo el administrador puede ver el dashboard"), respuesta.getBody().get("mensajes"));
    }
}