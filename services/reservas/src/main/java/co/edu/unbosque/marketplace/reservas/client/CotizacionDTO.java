package co.edu.unbosque.marketplace.reservas.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionDTO(
        String id,
        Long publicacionId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal total) {
}
