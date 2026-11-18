package co.edu.unbosque.marketplace.reservas.client;

import java.math.BigDecimal;
import java.util.UUID;

public record PagoRespuestaDTO(
        UUID id,
        String stripePaymentIntentId,
        BigDecimal monto,
        String estado
) {
}