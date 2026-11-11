package co.edu.unbosque.marketplace.cotizacion.exception;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Convierte los errores en msj mas claros para el front
@RestControllerAdvice
public class ControlErrores {

    // CA-106 porcentaje negativo, mayor a 100 o vacío
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensajes", mensajes));
    }

    // CA-106 cuando mandan letras u otra cosa que no es un número
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarFormato(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensajes", List.of("Los datos enviados no tienen el formato correcto")));
    }

    // No hay sesión
    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<Map<String, Object>> manejarAutenticacion(AutenticacionException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensajes", List.of(e.getMessage())));
    }

    // No mandaron el token
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> manejarSinToken(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensajes", List.of("Debe iniciar sesión")));
    }

    // No es administrador
    @ExceptionHandler(PermisoException.class)
    public ResponseEntity<Map<String, Object>> manejarPermiso(PermisoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("mensajes", List.of(e.getMessage())));
    }
}