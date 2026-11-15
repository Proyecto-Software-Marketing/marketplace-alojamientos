package co.edu.unbosque.marketplace.reportes.service;

import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.reportes.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reportes.dto.DashboardResponse;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasPublicaciones;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasReservas;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasUsuarios;

// Arma el dashboard del administrador 
@Service
public class DashboardService {

    private final ServiciosExternos serviciosExternos;

    public DashboardService(ServiciosExternos serviciosExternos) {
        this.serviciosExternos = serviciosExternos;
    }

    public DashboardResponse consultar(String autorizacion) {
    
        serviciosExternos.verificarAdministrador(autorizacion);

        // CA-109
        EstadisticasUsuarios usuarios = serviciosExternos.estadisticasUsuarios();
        EstadisticasPublicaciones publicaciones = serviciosExternos.estadisticasPublicaciones();
        EstadisticasReservas reservas = serviciosExternos.estadisticasReservas();

        // CA-108
        return new DashboardResponse(
                usuarios.getTotal(),
                publicaciones.getActivas(),
                reservas.getPorEstado(),
                reservas.getIngresosEstimados(),
                reservas.getCiudadesMayorDemanda());
    }
}