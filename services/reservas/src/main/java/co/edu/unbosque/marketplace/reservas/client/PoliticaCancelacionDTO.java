package co.edu.unbosque.marketplace.reservas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

// Política de cancelación de la publicación (Policy en el contrato de Alojamientos)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PoliticaCancelacionDTO(
        String nombre,
        BigDecimal porcentajeDevolucion,
        Boolean permiteLiberacionPeriodo) {
}
