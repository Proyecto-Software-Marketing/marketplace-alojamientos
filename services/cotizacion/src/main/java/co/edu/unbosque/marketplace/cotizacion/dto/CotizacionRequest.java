package co.edu.unbosque.marketplace.cotizacion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Lo que manda el huésped para cotizar (QuoteRequest en el contrato). HU-22, decisión 3
public class CotizacionRequest {

    @NotNull(message = "La publicación es obligatoria")
    private Long publicacionId;

    @NotNull(message = "La fecha de llegada es obligatoria")
    private LocalDate fechaLlegada;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDate fechaSalida;

    @NotNull(message = "La cantidad de huéspedes es obligatoria")
    @Min(value = 1, message = "Debe haber al menos un huésped")
    private Integer cantidadHuespedes;

    // Nombres de los servicios adicionales que elige el huésped (CA-129); es opcional
    private List<String> serviciosAdicionales = new ArrayList<>();

    public CotizacionRequest() {
    }

    public CotizacionRequest(Long publicacionId, LocalDate fechaLlegada, LocalDate fechaSalida,
            Integer cantidadHuespedes, List<String> serviciosAdicionales) {
        this.publicacionId = publicacionId;
        this.fechaLlegada = fechaLlegada;
        this.fechaSalida = fechaSalida;
        this.cantidadHuespedes = cantidadHuespedes;
        setServiciosAdicionales(serviciosAdicionales);
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(Long publicacionId) {
        this.publicacionId = publicacionId;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public void setFechaLlegada(LocalDate fechaLlegada) {
        this.fechaLlegada = fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Integer getCantidadHuespedes() {
        return cantidadHuespedes;
    }

    public void setCantidadHuespedes(Integer cantidadHuespedes) {
        this.cantidadHuespedes = cantidadHuespedes;
    }

    public List<String> getServiciosAdicionales() {
        return serviciosAdicionales;
    }

    public void setServiciosAdicionales(List<String> serviciosAdicionales) {
        this.serviciosAdicionales = serviciosAdicionales == null ? new ArrayList<>() : serviciosAdicionales;
    }
}
