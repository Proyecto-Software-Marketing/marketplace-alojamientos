package co.edu.unbosque.marketplace.busqueda.dto;

import java.time.LocalDate;

// Lo que le respondemos al huesped cuando abre una publicacion
public class DetalleResponse {

    private PublicacionDetalle publicacion;
    private LocalDate llegada;
    private LocalDate salida;
    private Integer huespedes;

    public DetalleResponse(PublicacionDetalle publicacion, LocalDate llegada, LocalDate salida, Integer huespedes) {
        this.publicacion = publicacion;
        this.llegada = llegada;
        this.salida = salida;
        this.huespedes = huespedes;
    }

    public PublicacionDetalle getPublicacion() { return publicacion; }
    public LocalDate getLlegada() { return llegada; }
    public LocalDate getSalida() { return salida; }
    public Integer getHuespedes() { return huespedes; }
}