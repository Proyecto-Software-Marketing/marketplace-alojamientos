package co.edu.unbosque.marketplace.cotizacion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

// Servicio adicional con su costo: lo ofrece la publicación (quote-data) y va en la cotización (CA-060)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicioAdicionalCotizado {

    private String nombre;
    private BigDecimal costo;

    public ServicioAdicionalCotizado() {
    }

    public ServicioAdicionalCotizado(String nombre, BigDecimal costo) {
        this.nombre = nombre;
        this.costo = costo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }
}
