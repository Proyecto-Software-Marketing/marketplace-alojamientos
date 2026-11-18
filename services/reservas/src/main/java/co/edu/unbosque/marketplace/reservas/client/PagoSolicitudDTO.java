package co.edu.unbosque.marketplace.reservas.client;

import java.math.BigDecimal;

public record PagoSolicitudDTO(
        BigDecimal monto,
        String moneda,
        String metodoPago
) {
}