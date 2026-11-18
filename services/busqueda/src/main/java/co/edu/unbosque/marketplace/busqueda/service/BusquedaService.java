package co.edu.unbosque.marketplace.busqueda.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.busqueda.client.ServiciosExternos;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaResponse;
import co.edu.unbosque.marketplace.busqueda.dto.DetalleResponse;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionDetalle;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.exception.BusquedaException;
import co.edu.unbosque.marketplace.busqueda.exception.PublicacionNoEncontradaException;

// la logica de la busqueda
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
       
        if (datos.getPrecioMinimo() != null && datos.getPrecioMaximo() != null
                && datos.getPrecioMinimo().compareTo(datos.getPrecioMaximo()) > 0) {
            throw new BusquedaException("El precio mínimo no puede ser mayor que el precio máximo");
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
            // CA-056
            if (!cumpleFiltros(publicacion, datos)) {
                continue;
            }
            // CA-053
            if (!serviciosExternos.estaLibre(publicacion.getId(), datos.getLlegada(), datos.getSalida())) {
                continue;
            }
            resultados.add(publicacion);
        }

        // CA-057
        if (resultados.isEmpty()) {
            return new BusquedaResponse(resultados, "No hay alojamientos que cumplan con la búsqueda");
        }
        return new BusquedaResponse(resultados, null);
    }
     //detalle de una publicacion para compararla antes de cotizar
    public DetalleResponse detalle(Long idPublicacion, LocalDate llegada, LocalDate salida, Integer huespedes) {
        PublicacionDetalle publicacion = serviciosExternos.obtenerDetalle(idPublicacion);

        
        if (publicacion == null || !"ACTIVA".equals(publicacion.getEstado())) {
            throw new PublicacionNoEncontradaException("La publicación no existe o no está disponible");
        }
       
        return new DetalleResponse(publicacion, llegada, salida, huespedes);
    }

    // Revisa los filtros uno por uno y si el huesped no lleno un filtro ese no se aplica
    private boolean cumpleFiltros(PublicacionResumen publicacion, BusquedaRequest datos) {
       
        if (datos.getTipo() != null && !datos.getTipo().isBlank()
                && !datos.getTipo().equalsIgnoreCase(publicacion.getTipo())) {
            return false;
        }
     
        if (datos.getPrecioMinimo() != null
                && publicacion.getPrecioNoche().compareTo(datos.getPrecioMinimo()) < 0) {
            return false;
        }
   
        if (datos.getPrecioMaximo() != null
                && publicacion.getPrecioNoche().compareTo(datos.getPrecioMaximo()) > 0) {
            return false;
        }

        if (datos.getServicios() != null && !datos.getServicios().isEmpty()) {
            if (publicacion.getServicios() == null || !publicacion.getServicios().containsAll(datos.getServicios())) {
                return false;
            }
        }
        return true;
    }
    
  
    public List<PublicacionResumen> destacadas() {
        List<PublicacionResumen> lista = new ArrayList<>();
        
        for (PublicacionResumen publicacion : serviciosExternos.buscarPublicacionesActivas("")) {
            if ("ACTIVA".equals(publicacion.getEstado())) {
                lista.add(publicacion);
            }
            if (lista.size() == 6) {
                break;
            }
        }
        return lista;
    }
}