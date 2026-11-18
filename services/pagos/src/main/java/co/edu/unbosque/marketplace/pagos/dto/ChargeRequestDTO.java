package co.edu.unbosque.marketplace.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChargeRequestDTO(

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotBlank(message = "La moneda es obligatoria")
        String moneda,

        @NotBlank(message = "El metodo de pago es obligatorio")
        String metodoPago
) {
}