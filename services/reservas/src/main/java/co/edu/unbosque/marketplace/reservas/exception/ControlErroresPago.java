package co.edu.unbosque.marketplace.reservas.exception;

import co.edu.unbosque.marketplace.reservas.client.PagoNoDisponibleReservaException;
import co.edu.unbosque.marketplace.reservas.client.PagoRechazadoReservaException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ControlErroresPago {

    @ExceptionHandler(PagoRechazadoReservaException.class)
    public ResponseEntity<Map<String, String>> pagoRechazado(
            PagoRechazadoReservaException error) {

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of(
                        "codigo", "PAGO_RECHAZADO",
                        "mensaje", error.getMessage()
                ));
    }

    @ExceptionHandler(PagoNoDisponibleReservaException.class)
    public ResponseEntity<Map<String, String>> pagoNoDisponible(
            PagoNoDisponibleReservaException error) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "codigo", "PAGO_NO_DISPONIBLE",
                        "mensaje", error.getMessage()
                ));
    }
}