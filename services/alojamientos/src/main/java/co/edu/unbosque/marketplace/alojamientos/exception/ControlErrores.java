package co.edu.unbosque.marketplace.alojamientos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

// Convierte cada excepción en su código HTTP con el formato {"mensajes": [...]} del equipo (decisión 10)
@RestControllerAdvice
public class ControlErrores {

    // 400: datos de la publicación que no cumplen las reglas (CA-021 y decisiones 6 a 8).
    // Varias reglas de un mismo campo comparten mensaje: se muestra una sola vez
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> manejarValidacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensajes", mensajes));
    }

    // 400: JSON mal formado o un dato con el tipo equivocado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> manejarFormato(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato correcto");
    }

    // 400: el id de la URL no es un número, por ejemplo /internal/listings/abc/owner
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> manejarTipo(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "El identificador enviado no es válido");
    }

    // 400: falta un parámetro obligatorio, por ejemplo idAnfitrion en /interno/publicaciones/pausar
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, List<String>>> manejarSinParametro(MissingServletRequestParameterException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + e.getParameterName());
    }

    // 400: regla de la publicación verificada en el servicio (CA-021)
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, List<String>>> manejarSolicitud(SolicitudInvalidaException e) {
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

    // 403: el usuario no es anfitrión (CA-022)
    @ExceptionHandler(PermisoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarPermiso(PermisoException e) {
        return respuesta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // 404: la publicación no existe
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarNoEncontrado(NoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 409: el estado de la publicación no permite el cambio (HU-09, decisión 4)
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, List<String>>> manejarConflicto(ConflictoException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    // 503: Identidad no respondió
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, List<String>>> manejarServicioCaido(ServicioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

// HU-12 (CA-036): publicacion incompleta para revision.
@ExceptionHandler(PublicacionIncompletaException.class)
public ResponseEntity<Map<String, List<String>>> manejarPublicacionIncompleta(
        PublicacionIncompletaException e) {
    return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
}

    private ResponseEntity<Map<String, List<String>>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensajes", List.of(mensaje)));
    }
}
