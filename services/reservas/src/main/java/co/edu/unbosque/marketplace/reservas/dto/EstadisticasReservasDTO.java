package co.edu.unbosque.marketplace.reservas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EstadisticasReservasDTO(
        Map<String, Long> porEstado,
        BigDecimal ingresosEstimados,
        List<Map<String, Object>> ciudadesMayorDemanda) {
}