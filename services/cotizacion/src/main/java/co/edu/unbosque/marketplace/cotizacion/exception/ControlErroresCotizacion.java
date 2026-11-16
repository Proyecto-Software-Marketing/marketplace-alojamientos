package co.edu.unbosque.marketplace.cotizacion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

// HU-22: errores de la cotización con el formato {"mensajes": [...]}.
// Los de validación, sesión y permiso los sigue manejando ControlErrores
@RestControllerAdvice
public class ControlErroresCotizacion {

    // Fechas inválidas o servicio adicional que la publicación no ofrece
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, Object>> manejarSolicitud(SolicitudInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // La publicación o la cotización no existe
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(NoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // La publicación no está ACTIVA
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> manejarConflicto(ConflictoException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    // CA-063 más huéspedes que la capacidad
    @ExceptionHandler(CapacidadExcedidaException.class)
    public ResponseEntity<Map<String, Object>> manejarCapacidad(CapacidadExcedidaException e) {
        // 422 se escribe como número: el nombre de la constante cambió entre versiones de Spring
        return ResponseEntity.status(422).body(Map.of("mensajes", List.of(e.getMessage())));
    }

    // Identidad o Alojamientos no respondieron
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarServicioCaido(ServicioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensajes", List.of(mensaje)));
    }
}
