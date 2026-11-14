package co.edu.unbosque.marketplace.busqueda.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.busqueda.client.ServiciosExternos;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaResponse;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.exception.BusquedaException;

//logica de la busqueda
@Service
public class BusquedaService {

    private final ServiciosExternos serviciosExternos;

    public BusquedaService(ServiciosExternos serviciosExternos) {
        this.serviciosExternos = serviciosExternos;
    }

    public BusquedaResponse buscar(BusquedaRequest datos) {
        // CA-054
        if (!datos.getSalida().isAfter(datos.getLlegada())) {
            throw new BusquedaException("La fecha de salida debe ser posterior a la fecha de llegada");
        }

        List<PublicacionResumen> publicaciones = serviciosExternos.buscarPublicacionesActivas(datos.getCiudad());
        List<PublicacionResumen> resultados = new ArrayList<>();

        for (PublicacionResumen publicacion : publicaciones) {
            // CA-055
            if (!"ACTIVA".equals(publicacion.getEstado())) {
                continue;
            }
            // CA-053
            if (publicacion.getCapacidad() == null || publicacion.getCapacidad() < datos.getHuespedes()) {
                continue;
            }
            // CA-053
            if (!serviciosExternos.estaLibre(publicacion.getId(), datos.getLlegada(), datos.getSalida())) {
                continue;
            }
            resultados.add(publicacion);
        }

        return new BusquedaResponse(resultados);
    }
}