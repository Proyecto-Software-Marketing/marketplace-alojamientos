package co.edu.unbosque.marketplace.busqueda.dto;

import java.util.List;

// Detalle completo de una publicacion que nos manda Alojamientos
// Hereda de PublicacionResumen los datos basicos (id, titulo, ciudad, tipo, capacidad, precio, estado y servicios)
// y agrega lo que solo se ve en el detalle
public class PublicacionDetalle extends PublicacionResumen {

    private String descripcion;
    private List<String> reglas;
    private List<String> imagenes;
    private String politicaCancelacion;

    public PublicacionDetalle() {
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<String> getReglas() { return reglas; }
    public void setReglas(List<String> reglas) { this.reglas = reglas; }

    public List<String> getImagenes() { return imagenes; }
    public void setImagenes(List<String> imagenes) { this.imagenes = imagenes; }

    public String getPoliticaCancelacion() { return politicaCancelacion; }
    public void setPoliticaCancelacion(String politicaCancelacion) { this.politicaCancelacion = politicaCancelacion; }
}