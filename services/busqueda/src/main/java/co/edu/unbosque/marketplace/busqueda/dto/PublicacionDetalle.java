package co.edu.unbosque.marketplace.busqueda.dto;

import java.util.List;
import java.util.Map;

public class PublicacionDetalle extends PublicacionResumen {

    private String descripcion;
    private List<String> reglas;
    private List<String> imagenes;
    private String politicaCancelacion;
    private List<Map<String, Object>> serviciosAdicionales;

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
  public List<Map<String, Object>> getServiciosAdicionales() { return serviciosAdicionales; }
    public void setServiciosAdicionales(List<Map<String, Object>> serviciosAdicionales) { this.serviciosAdicionales = serviciosAdicionales; }
}