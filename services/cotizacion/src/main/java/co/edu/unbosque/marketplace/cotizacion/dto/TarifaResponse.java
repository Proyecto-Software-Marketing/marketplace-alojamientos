package co.edu.unbosque.marketplace.cotizacion.dto;

import java.math.BigDecimal;

// El porcentaje que se está usando
public class TarifaResponse {

    private BigDecimal porcentaje;

    public TarifaResponse(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }
}