package co.edu.unbosque.marketplace.busqueda.dto;

import java.math.BigDecimal;
import java.util.List;

// Detalle completo de una publicacion 
public class PublicacionDetalle {

    private Long id;
    private String titulo;
    private String descripcion;
    private String ciudad;
    private String tipo;
    private Integer capacidad;
    private BigDecimal precioNoche;
    private String estado;
    private List<String> servicios;
    private List<String> reglas;
    private List<String> imagenes;
    private String politicaCancelacion;

    public PublicacionDetalle() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public BigDecimal getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(BigDecimal precioNoche) { this.precioNoche = precioNoche; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<String> getServicios() { return servicios; }
    public void setServicios(List<String> servicios) { this.servicios = servicios; }

    public List<String> getReglas() { return reglas; }
    public void setReglas(List<String> reglas) { this.reglas = reglas; }

    public List<String> getImagenes() { return imagenes; }
    public void setImagenes(List<String> imagenes) { this.imagenes = imagenes; }

    public String getPoliticaCancelacion() { return politicaCancelacion; }
    public void setPoliticaCancelacion(String politicaCancelacion) { this.politicaCancelacion = politicaCancelacion; }
}