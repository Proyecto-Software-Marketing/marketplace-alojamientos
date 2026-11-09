package co.edu.unbosque.marketplace.identidad.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

// Convierte los errores en msj mas claros para el front
@RestControllerAdvice
public class ControlErrores {

    // Correo repetido o rol Administrador (CA-002, CA-003)
    @ExceptionHandler(RegistroException.class)
    public ResponseEntity<Map<String, Object>> manejarRegistro(RegistroException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensajes", List.of(e.getMessage())));
    }

    // Datos que no cumplen las validaciones del formulario
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensajes", mensajes));
    }

    // Datos con formato incorrecto, por ejemplo un rol que no existe
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarFormato(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensajes", List.of("Los datos enviados no tienen el formato correcto")));
    }

    // Credenciales incorrectas o sesión no válida (CA-005, CA-006)
    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<Map<String, Object>> manejarAutenticacion(AutenticacionException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensajes", List.of(e.getMessage())));
    }

    // Se pidió una ruta protegida sin enviar el token
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> manejarSinToken(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensajes", List.of("Debe iniciar sesión")));
    }
}