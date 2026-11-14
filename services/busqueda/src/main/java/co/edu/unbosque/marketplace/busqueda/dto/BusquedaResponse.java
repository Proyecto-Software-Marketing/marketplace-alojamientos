package co.edu.unbosque.marketplace.busqueda.dto;

import java.util.List;

// la lista de alojamientos encontrados
public class BusquedaResponse {

    private List<PublicacionResumen> resultados;

    public BusquedaResponse(List<PublicacionResumen> resultados) {
        this.resultados = resultados;
    }

    public List<PublicacionResumen> getResultados() {
        return resultados;
    }
}