package co.edu.unbosque.marketplace.cotizacion.dto;

import co.edu.unbosque.marketplace.cotizacion.model.Cotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Cotización desglosada (Quote en el contrato). CA-060; decisión 10.
// Incluye publicación, fechas, huéspedes y total, que es lo que lee Reservas (su CotizacionDTO también es un record)
public record CotizacionResponse(
        String id,
        Long publicacionId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        int cantidadHuespedes,
        int noches,
        BigDecimal precioPorNoche,
        BigDecimal precioBase,
        BigDecimal tarifaLimpieza,
        List<ServicioAdicionalCotizado> serviciosAdicionales,
        int porcentajeDescuento,
        BigDecimal descuento,
        BigDecimal porcentajeTarifaServicio,
        BigDecimal tarifaServicio,
        BigDecimal total) {

    public static CotizacionResponse desde(Cotizacion cotizacion) {
        return new CotizacionResponse(
                cotizacion.getId(),
                cotizacion.getPublicacionId(),
                cotizacion.getFechaLlegada(),
                cotizacion.getFechaSalida(),
                cotizacion.getCantidadHuespedes(),
                cotizacion.getNoches(),
                cotizacion.getPrecioPorNoche(),
                cotizacion.getPrecioBase(),
                cotizacion.getTarifaLimpieza(),
                cotizacion.getServiciosAdicionales().stream()
                        .map(servicio -> new ServicioAdicionalCotizado(servicio.getNombre(), servicio.getCosto()))
                        .toList(),
                cotizacion.getPorcentajeDescuento(),
                cotizacion.getDescuento(),
                cotizacion.getPorcentajeTarifaServicio(),
                cotizacion.getTarifaServicio(),
                cotizacion.getTotal());
    }
}
