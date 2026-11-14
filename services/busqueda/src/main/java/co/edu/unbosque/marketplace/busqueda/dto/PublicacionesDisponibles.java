package co.edu.unbosque.marketplace.busqueda.dto;

import java.util.List;


public class PublicacionesDisponibles {

    private List<Long> disponibles;

    public PublicacionesDisponibles() {
    }

    public List<Long> getDisponibles() { return disponibles; }
    public void setDisponibles(List<Long> disponibles) { this.disponibles = disponibles; }
}