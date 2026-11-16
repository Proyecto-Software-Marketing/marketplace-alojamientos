package co.edu.unbosque.marketplace.alojamientos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// Cuerpo de PUT /listings/{id}/pricing (PricingInput en el contrato). Reemplaza toda la configuración de precios.
// Los números se reciben como BigDecimal para rechazar decimales en vez de recortarlos (CA-031)
public record TarifasRequestDTO(
        // Decisión 3
        @NotNull(message = TarifasRequestDTO.MENSAJE_LIMPIEZA)
        @DecimalMin(value = "0", message = TarifasRequestDTO.MENSAJE_LIMPIEZA)
        @Digits(integer = 9, fraction = 0, message = TarifasRequestDTO.MENSAJE_LIMPIEZA)
        BigDecimal tarifaLimpieza,

        // Decisión 4
        @NotNull(message = TarifasRequestDTO.MENSAJE_PORCENTAJE)
        @DecimalMin(value = "0", message = TarifasRequestDTO.MENSAJE_PORCENTAJE)
        @DecimalMax(value = "90", message = TarifasRequestDTO.MENSAJE_PORCENTAJE)
        @Digits(integer = 2, fraction = 0, message = TarifasRequestDTO.MENSAJE_PORCENTAJE)
        BigDecimal porcentajeDescuentoEstanciaLarga,

        // Decisión 5: obligatoria solo si hay descuento (se verifica en el servicio)
        @DecimalMin(value = "2", message = TarifasRequestDTO.MENSAJE_NOCHES)
        @DecimalMax(value = "365", message = TarifasRequestDTO.MENSAJE_NOCHES)
        @Digits(integer = 3, fraction = 0, message = TarifasRequestDTO.MENSAJE_NOCHES)
        BigDecimal nochesMinimasDescuento,

        // Decisión 6
        @NotNull(message = TarifasRequestDTO.MENSAJE_ADICIONALES)
        @Size(max = 10, message = TarifasRequestDTO.MENSAJE_CANTIDAD_ADICIONALES)
        @Valid
        List<ServicioAdicionalDTO> serviciosAdicionales) {

    public static final String MENSAJE_LIMPIEZA =
            "La tarifa de limpieza debe ser un valor entero en pesos, de 0 o más y de máximo 9 dígitos";
    public static final String MENSAJE_PORCENTAJE =
            "El porcentaje de descuento por estancia larga debe ser un número entero entre 0 y 90";
    public static final String MENSAJE_NOCHES =
            "Las noches mínimas para el descuento deben ser un número entero entre 2 y 365";
    public static final String MENSAJE_ADICIONALES = "La lista de servicios adicionales es obligatoria";
    public static final String MENSAJE_CANTIDAD_ADICIONALES = "Se pueden registrar máximo 10 servicios adicionales";
}
