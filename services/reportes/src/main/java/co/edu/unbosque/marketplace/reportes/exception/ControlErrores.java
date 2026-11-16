package co.edu.unbosque.marketplace.reportes.exception;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControlErrores {

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<Map<String, List<String>>> errorAutenticacion(AutenticacionException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensajes", List.of(e.getMessage())));
    }

    // CA-110
    @ExceptionHandler(PermisoException.class)
    public ResponseEntity<Map<String, List<String>>> errorPermiso(PermisoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensajes", List.of(e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> errorValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, mensajes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> errorFormato(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, List.of("El tipo de reporte o el formato no son válidos"));
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, List<String>>> errorServicioCaido(ServicioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, List.of(e.getMessage()));
    }

    private ResponseEntity<Map<String, List<String>>> respuesta(HttpStatus estado, List<String> mensajes) {
        return ResponseEntity.status(estado).body(Map.of("mensajes", mensajes));
    }
}
