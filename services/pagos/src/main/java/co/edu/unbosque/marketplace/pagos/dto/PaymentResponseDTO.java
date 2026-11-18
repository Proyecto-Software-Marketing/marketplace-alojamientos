package co.edu.unbosque.marketplace.pagos.dto;

import co.edu.unbosque.marketplace.pagos.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponseDTO(
        UUID id,
        String stripePaymentIntentId,
        BigDecimal monto,
        PaymentStatus estado
) {
}