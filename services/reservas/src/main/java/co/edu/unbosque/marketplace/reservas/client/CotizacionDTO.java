package co.edu.unbosque.marketplace.reservas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

// Datos de la cotización que necesita Reservas (GET /quotes/{id} de Cotización)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CotizacionDTO(
        String id,
        Long publicacionId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal total) {
}
