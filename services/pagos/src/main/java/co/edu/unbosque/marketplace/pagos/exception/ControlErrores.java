package co.edu.unbosque.marketplace.pagos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ControlErrores {

    @ExceptionHandler(PagoRechazadoException.class)
    public ResponseEntity<Map<String, String>> pagoRechazado(
            PagoRechazadoException e) {

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of(
                        "codigo", "PAGO_RECHAZADO",
                        "mensaje", e.getMessage()
                ));
    }

    @ExceptionHandler(ServicioPagoException.class)
    public ResponseEntity<Map<String, String>> servicioNoDisponible(
            ServicioPagoException e) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "codigo", "SERVICIO_PAGOS_NO_DISPONIBLE",
                        "mensaje", e.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(
            IllegalArgumentException e) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "codigo", "SOLICITUD_INVALIDA",
                        "mensaje", e.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(
            MethodArgumentNotValidException e) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "codigo", "DATOS_INVALIDOS",
                        "mensaje", "Los datos del pago no son validos."
                ));
    }
}