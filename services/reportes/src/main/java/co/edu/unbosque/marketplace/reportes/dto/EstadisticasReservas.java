package co.edu.unbosque.marketplace.reportes.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EstadisticasReservas {

    private Map<String, Long> porEstado = new HashMap<>();
    private BigDecimal ingresosEstimados = BigDecimal.ZERO;
    private List<CiudadDemanda> ciudadesMayorDemanda = List.of();

    public EstadisticasReservas() {
    }

    public Map<String, Long> getPorEstado() { return porEstado; }
    public void setPorEstado(Map<String, Long> porEstado) { this.porEstado = porEstado; }

    public BigDecimal getIngresosEstimados() { return ingresosEstimados; }
    public void setIngresosEstimados(BigDecimal ingresosEstimados) { this.ingresosEstimados = ingresosEstimados; }

    public List<CiudadDemanda> getCiudadesMayorDemanda() { return ciudadesMayorDemanda; }
    public void setCiudadesMayorDemanda(List<CiudadDemanda> ciudadesMayorDemanda) { this.ciudadesMayorDemanda = ciudadesMayorDemanda; }
}