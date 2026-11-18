package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;

// Lo que recibe Búsqueda en GET /interno/publicaciones/activas (su clase PublicacionResumen).
// Decisión 2: usa los nombres de Búsqueda (tipo, precioNoche), no los del contrato público
// imagenPrincipal: la primera imagen, para mostrarla en las tarjetas del inicio
public record PublicacionResumenDTO(
        Long id,
        String titulo,
        String ciudad,
        String tipo,
        Integer capacidad,
        BigDecimal precioNoche,
        String estado,
        List<String> servicios,
        String imagenPrincipal) {

    public static PublicacionResumenDTO desde(Publicacion publicacion) {
        List<String> imagenes = publicacion.getImagenes();
        return new PublicacionResumenDTO(
                publicacion.getId(),
                publicacion.getTitulo(),
                publicacion.getCiudad(),
                publicacion.getTipo().name(),
                publicacion.getCapacidad(),
                publicacion.getPrecioNoche(),
                publicacion.getEstado().name(),
                publicacion.getServicios(),
                imagenes.isEmpty() ? null : imagenes.get(0));
    }
}