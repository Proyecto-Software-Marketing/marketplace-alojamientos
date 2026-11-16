package co.edu.unbosque.marketplace.cotizacion.dto;

import co.edu.unbosque.marketplace.cotizacion.model.Cotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Cotización desglosada (Quote en el contrato). CA-060; decisión 10.
// Incluye publicación, fechas, huéspedes y total, que es lo que lee Reservas
public class CotizacionResponse {

    private final String id;
    private final Long publicacionId;
    private final LocalDate fechaLlegada;
    private final LocalDate fechaSalida;
    private final int cantidadHuespedes;
    private final int noches;
    private final BigDecimal precioPorNoche;
    private final BigDecimal precioBase;
    private final BigDecimal tarifaLimpieza;
    private final List<ServicioAdicionalCotizado> serviciosAdicionales;
    private final int porcentajeDescuento;
    private final BigDecimal descuento;
    private final BigDecimal porcentajeTarifaServicio;
    private final BigDecimal tarifaServicio;
    private final BigDecimal total;

    public CotizacionResponse(Cotizacion cotizacion) {
        this.id = cotizacion.getId();
        this.publicacionId = cotizacion.getPublicacionId();
        this.fechaLlegada = cotizacion.getFechaLlegada();
        this.fechaSalida = cotizacion.getFechaSalida();
        this.cantidadHuespedes = cotizacion.getCantidadHuespedes();
        this.noches = cotizacion.getNoches();
        this.precioPorNoche = cotizacion.getPrecioPorNoche();
        this.precioBase = cotizacion.getPrecioBase();
        this.tarifaLimpieza = cotizacion.getTarifaLimpieza();
        this.serviciosAdicionales = cotizacion.getServiciosAdicionales().stream()
                .map(servicio -> new ServicioAdicionalCotizado(servicio.getNombre(), servicio.getCosto()))
                .toList();
        this.porcentajeDescuento = cotizacion.getPorcentajeDescuento();
        this.descuento = cotizacion.getDescuento();
        this.porcentajeTarifaServicio = cotizacion.getPorcentajeTarifaServicio();
        this.tarifaServicio = cotizacion.getTarifaServicio();
        this.total = cotizacion.getTotal();
    }

    public String getId() {
        return id;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public int getCantidadHuespedes() {
        return cantidadHuespedes;
    }

    public int getNoches() {
        return noches;
    }

    public BigDecimal getPrecioPorNoche() {
        return precioPorNoche;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public BigDecimal getTarifaLimpieza() {
        return tarifaLimpieza;
    }

    public List<ServicioAdicionalCotizado> getServiciosAdicionales() {
        return serviciosAdicionales;
    }

    public int getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public BigDecimal getPorcentajeTarifaServicio() {
        return porcentajeTarifaServicio;
    }

    public BigDecimal getTarifaServicio() {
        return tarifaServicio;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
