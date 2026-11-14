package co.edu.unbosque.marketplace.busqueda.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que el huesped llena para buscar alojamiento
public class BusquedaRequest {

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    // las fechas en este formato: 2026-12-01
    @NotNull(message = "La fecha de llegada es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate llegada;

    @NotNull(message = "La fecha de salida es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate salida;

    @NotNull(message = "El número de huéspedes es obligatorio")
    @Min(value = 1, message = "Debe haber al menos un huésped")
    private Integer huespedes;

    public BusquedaRequest() {
    }

    public BusquedaRequest(String ciudad, LocalDate llegada, LocalDate salida, Integer huespedes) {
        this.ciudad = ciudad;
        this.llegada = llegada;
        this.salida = salida;
        this.huespedes = huespedes;
    }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public LocalDate getLlegada() { return llegada; }
    public void setLlegada(LocalDate llegada) { this.llegada = llegada; }

    public LocalDate getSalida() { return salida; }
    public void setSalida(LocalDate salida) { this.salida = salida; }

    public Integer getHuespedes() { return huespedes; }
    public void setHuespedes(Integer huespedes) { this.huespedes = huespedes; }
}
