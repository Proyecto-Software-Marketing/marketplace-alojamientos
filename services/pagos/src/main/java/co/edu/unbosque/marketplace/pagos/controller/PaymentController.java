package co.edu.unbosque.marketplace.pagos.controller;

import co.edu.unbosque.marketplace.pagos.dto.ChargeRequestDTO;
import co.edu.unbosque.marketplace.pagos.dto.PaymentResponseDTO;
import co.edu.unbosque.marketplace.pagos.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> cobrar(
            @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @Valid @RequestBody ChargeRequestDTO solicitud) {

        PaymentResponseDTO resultado =
                paymentService.cobrar(claveIdempotencia, solicitud);

        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    // MKT-41: recuperar un pago aprobado usando su clave de idempotencia.
    @GetMapping("/{claveIdempotencia}")
    public ResponseEntity<PaymentResponseDTO> consultar(
            @PathVariable String claveIdempotencia) {

        return paymentService.consultarPorClave(claveIdempotencia)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}