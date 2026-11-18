package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.PoliticaCancelacion;
import java.math.BigDecimal;

public record PoliticaCancelacionDTO(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal porcentajeDevolucion,
        boolean permiteLiberacionPeriodo) {

    public static PoliticaCancelacionDTO desde(PoliticaCancelacion politica) {
        return new PoliticaCancelacionDTO(
                politica.getId(),
                politica.getNombre(),
                politica.getDescripcion(),
                politica.getPorcentajeDevolucion(),
                politica.isPermiteLiberacionPeriodo());
    }
}