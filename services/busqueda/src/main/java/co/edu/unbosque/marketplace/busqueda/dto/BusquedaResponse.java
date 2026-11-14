package co.edu.unbosque.marketplace.busqueda.dto;

import java.util.List;

// la lista de alojamientos y un mensaje si no hay ninguno
public class BusquedaResponse {

    private List<PublicacionResumen> resultados;
    private String mensaje;

    public BusquedaResponse(List<PublicacionResumen> resultados, String mensaje) {
        this.resultados = resultados;
        this.mensaje = mensaje;
    }

    public List<PublicacionResumen> getResultados() {
        return resultados;
    }

    public String getMensaje() {
        return mensaje;
    }
}