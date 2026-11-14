package co.edu.unbosque.marketplace.notificaciones.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

// Convierte cada excepción en su código HTTP con el formato {"mensajes": [...]} del equipo
@RestControllerAdvice
public class ControlErrores {

    // 400: falta un campo obligatorio (@NotNull / @NotBlank de NotificacionRequestDTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> manejarValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensajes", mensajes));
    }

    // 400: JSON mal formado o un id que no es número
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> manejarFormato(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato correcto");
    }

    // 400: tipo de evento desconocido o entidad que no corresponde al evento
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, List<String>>> manejarSolicitud(SolicitudInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, List<String>>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensajes", List.of(mensaje)));
    }
}
