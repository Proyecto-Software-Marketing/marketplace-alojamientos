package co.edu.unbosque.marketplace.cotizacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Lo que manda el admin para cambiar el porcentaje
public class TarifaRequest {

    // CA-106
    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje no puede ser mayor a 100")
    private BigDecimal porcentaje;

    public TarifaRequest() {
    }

    public TarifaRequest(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }
}