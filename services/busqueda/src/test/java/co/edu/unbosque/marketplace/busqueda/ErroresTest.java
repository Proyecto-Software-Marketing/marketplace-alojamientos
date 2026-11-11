package co.edu.unbosque.marketplace.busqueda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import co.edu.unbosque.marketplace.busqueda.client.ServiciosExternos;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.exception.BusquedaException;
import co.edu.unbosque.marketplace.busqueda.exception.ControlErrores;

@SpringBootTest
class ErroresTest {

 
    @Autowired
    private ServiciosExternos serviciosExternos;

    private final ControlErrores controlErrores = new ControlErrores();

    @Test
    void sinAlojamientosConfiguradoNoHayPublicaciones() {
        assertTrue(serviciosExternos.buscarPublicacionesActivas("Bogota").isEmpty());
    }

    @Test
    void sinDisponibilidadConfiguradoSeAsumeLibre() {
        assertTrue(serviciosExternos.estaLibre(1L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5)));
    }

    @Test
    void errorDeBusquedaResponde400() {
        ResponseEntity<Map<String, List<String>>> respuesta = controlErrores.errorBusqueda(new BusquedaException("Mensaje de prueba"));

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals(List.of("Mensaje de prueba"), respuesta.getBody().get("mensajes"));
    }

    @Test
    void camposMalosResponden400ConSusMensajes() {
        BindException error = new BindException(new BusquedaRequest(), "datos");
        error.addError(new FieldError("datos", "ciudad", "La ciudad es obligatoria"));
        error.addError(new FieldError("datos", "llegada", "abc", true, null, null, "formato malo"));

        ResponseEntity<Map<String, List<String>>> respuesta = controlErrores.errorCampos(error);

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals(List.of("La ciudad es obligatoria", "El valor de llegada no es válido"),
                respuesta.getBody().get("mensajes"));
    }
}