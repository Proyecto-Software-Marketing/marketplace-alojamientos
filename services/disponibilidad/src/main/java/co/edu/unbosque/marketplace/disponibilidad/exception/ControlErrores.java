package co.edu.unbosque.marketplace.disponibilidad.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

// Convierte cada excepción en su código HTTP con el formato {"mensajes": [...]} del equipo
@RestControllerAdvice
public class ControlErrores {

    // 400: falta una fecha (@NotNull de PeriodoRequestDTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> manejarValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensajes", mensajes));
    }

    // 400: JSON mal formado o una fecha escrita en otro formato
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> manejarFormato(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato correcto (fechas AAAA-MM-DD)");
    }

    // 400: un id de la URL que no es un número, por ejemplo /listings/abc/availability
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> manejarTipo(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "El identificador enviado no es válido");
    }

    // 400: CA-046 y decisión 3 de la HU-16
    @ExceptionHandler(FechasInvalidasException.class)
    public ResponseEntity<Map<String, List<String>>> manejarFechas(FechasInvalidasException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 401: token inválido o vencido
    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<Map<String, List<String>>> manejarAutenticacion(AutenticacionException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // 401: no enviaron el encabezado Authorization
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, List<String>>> manejarSinToken(MissingRequestHeaderException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
    }

    // 403: no es anfitrión o no es el dueño de la publicación
    @ExceptionHandler(PermisoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarPermiso(PermisoException e) {
        return respuesta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // 404: la publicación o el periodo no existe
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarNoEncontrado(NoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 409: CA-047 y decisión 2 de la HU-16
    @ExceptionHandler(ConflictoDisponibilidadException.class)
    public ResponseEntity<Map<String, List<String>>> manejarConflicto(ConflictoDisponibilidadException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    // 503: Identidad o Alojamientos no respondieron
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, List<String>>> manejarServicioCaido(ServicioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    private ResponseEntity<Map<String, List<String>>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensajes", List.of(mensaje)));
    }
}