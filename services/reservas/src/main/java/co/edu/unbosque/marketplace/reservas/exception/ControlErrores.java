package co.edu.unbosque.marketplace.reservas.exception;

import org.springframework.dao.DataIntegrityViolationException;
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

    // 400: falta un campo obligatorio
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> manejarValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensajes", mensajes));
    }

    // 400: JSON mal formado o formato de datos incorrecto
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> manejarFormato(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato correcto");
    }

    // 400: un parámetro o identificador no válido
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> manejarTipo(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "El identificador enviado no es válido");
    }

    // 400: datos obligatorios que no vienen en la cotización
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, List<String>>> manejarSolicitud(SolicitudInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 400: fechas inconsistentes
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
    public ResponseEntity<Map<String, List<String>>> manejarSinHeader(MissingRequestHeaderException e) {
        if ("Authorization".equalsIgnoreCase(e.getHeaderName())) {
            return respuesta(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el encabezado obligatorio: " + e.getHeaderName());
    }

    // 403: no tiene rol de huésped o no tiene permisos
    @ExceptionHandler(PermisoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarPermiso(PermisoException e) {
        return respuesta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // 404: reserva o cotización no encontrada
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarNoEncontrado(NoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 409: CA-065 ya no está disponible, o conflicto de reserva
    @ExceptionHandler(ConflictoReservaException.class)
    public ResponseEntity<Map<String, List<String>>> manejarConflicto(ConflictoReservaException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    // 409: dos solicitudes simultáneas con la misma clave de idempotencia
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, List<String>>> manejarDuplicado(DataIntegrityViolationException e) {
        return respuesta(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada");
    }

    // 503: algún microservicio externo no respondió
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, List<String>>> manejarServicioCaido(ServicioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    private ResponseEntity<Map<String, List<String>>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensajes", List.of(mensaje)));
    }
}
