package co.edu.unbosque.marketplace.reportes.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Indicadores del dashboard del administrador 
public class DashboardResponse {

    private long usuarios;
    private long publicacionesActivas;
    private Map<String, Long> reservasPorEstado;
    private BigDecimal ingresosEstimados;
    private List<CiudadDemanda> ciudadesMayorDemanda;

    public DashboardResponse(long usuarios, long publicacionesActivas, Map<String, Long> reservasPorEstado,
            BigDecimal ingresosEstimados, List<CiudadDemanda> ciudadesMayorDemanda) {
        this.usuarios = usuarios;
        this.publicacionesActivas = publicacionesActivas;
        this.reservasPorEstado = reservasPorEstado;
        this.ingresosEstimados = ingresosEstimados;
        this.ciudadesMayorDemanda = ciudadesMayorDemanda;
    }

    public long getUsuarios() { return usuarios; }
    public long getPublicacionesActivas() { return publicacionesActivas; }
    public Map<String, Long> getReservasPorEstado() { return reservasPorEstado; }
    public BigDecimal getIngresosEstimados() { return ingresosEstimados; }
    public List<CiudadDemanda> getCiudadesMayorDemanda() { return ciudadesMayorDemanda; }
}