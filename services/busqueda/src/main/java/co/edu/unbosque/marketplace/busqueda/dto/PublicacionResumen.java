package co.edu.unbosque.marketplace.busqueda.dto;

import java.math.BigDecimal;
import java.util.List;

// Datos de una publicacion que manda el servicio de Alojamientos
public class PublicacionResumen {

    private Long id;
    private String titulo;
    private String ciudad;
    private String tipo;
    private Integer capacidad;
    private BigDecimal precioNoche;
    private String estado;
    private List<String> servicios;

    public PublicacionResumen() {
    }

    public PublicacionResumen(Long id, String titulo, String ciudad, String tipo, Integer capacidad,
            BigDecimal precioNoche, String estado, List<String> servicios) {
        this.id = id;
        this.titulo = titulo;
        this.ciudad = ciudad;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.precioNoche = precioNoche;
        this.estado = estado;
        this.servicios = servicios;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

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
}