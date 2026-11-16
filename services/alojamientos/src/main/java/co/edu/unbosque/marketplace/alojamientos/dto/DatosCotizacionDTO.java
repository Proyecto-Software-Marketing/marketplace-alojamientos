package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;

// Respuesta de GET /internal/listings/{id}/quote-data (QuoteData en el contrato): lo que Cotización
// necesita para calcular el precio (HU-10, decisión 13). Las reglas de cálculo (decisiones 9 a 11)
// están en la descripción de la ruta en el contrato
public record DatosCotizacionDTO(
        Long publicacionId,
        String estado,
        Integer capacidad,
        BigDecimal precioPorNoche,
        BigDecimal tarifaLimpieza,
        Integer porcentajeDescuentoEstanciaLarga,
        Integer nochesMinimasDescuento,
        List<ServicioAdicionalDTO> serviciosAdicionales) {

    public static DatosCotizacionDTO desde(Publicacion publicacion) {
        return new DatosCotizacionDTO(
                publicacion.getId(),
                publicacion.getEstado().name(),
                publicacion.getCapacidad(),
                publicacion.getPrecioNoche(),
                publicacion.getTarifaLimpieza(),
                publicacion.getPorcentajeDescuento(),
                publicacion.getNochesMinimasDescuento(),
                publicacion.getServiciosAdicionales().stream().map(ServicioAdicionalDTO::desde).toList());
    }
}
