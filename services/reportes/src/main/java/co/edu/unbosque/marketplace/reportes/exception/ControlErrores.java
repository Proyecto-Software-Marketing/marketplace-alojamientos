package co.edu.unbosque.marketplace.reportes.exception;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}