package co.edu.unbosque.marketplace.cotizacion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Lo que responde Alojamientos en GET /internal/listings/{id}/quote-data (HU-10)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatosPublicacion {

    private Long publicacionId;
    private String estado;
    private Integer capacidad;
    private BigDecimal precioPorNoche;
    private BigDecimal tarifaLimpieza;
    private Integer porcentajeDescuentoEstanciaLarga;
    private Integer nochesMinimasDescuento;
    private List<ServicioAdicionalCotizado> serviciosAdicionales = new ArrayList<>();

    public DatosPublicacion() {
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(Long publicacionId) {
        this.publicacionId = publicacionId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getPrecioPorNoche() {
        return precioPorNoche;
    }

    public void setPrecioPorNoche(BigDecimal precioPorNoche) {
        this.precioPorNoche = precioPorNoche;
    }

    public BigDecimal getTarifaLimpieza() {
        return tarifaLimpieza;
    }

    public void setTarifaLimpieza(BigDecimal tarifaLimpieza) {
        this.tarifaLimpieza = tarifaLimpieza;
    }

    public Integer getPorcentajeDescuentoEstanciaLarga() {
        return porcentajeDescuentoEstanciaLarga;
    }

    public void setPorcentajeDescuentoEstanciaLarga(Integer porcentajeDescuentoEstanciaLarga) {
        this.porcentajeDescuentoEstanciaLarga = porcentajeDescuentoEstanciaLarga;
    }

    public Integer getNochesMinimasDescuento() {
        return nochesMinimasDescuento;
    }

    public void setNochesMinimasDescuento(Integer nochesMinimasDescuento) {
        this.nochesMinimasDescuento = nochesMinimasDescuento;
    }

    public List<ServicioAdicionalCotizado> getServiciosAdicionales() {
        return serviciosAdicionales;
    }

    public void setServiciosAdicionales(List<ServicioAdicionalCotizado> serviciosAdicionales) {
        this.serviciosAdicionales = serviciosAdicionales == null ? new ArrayList<>() : serviciosAdicionales;
    }
}
