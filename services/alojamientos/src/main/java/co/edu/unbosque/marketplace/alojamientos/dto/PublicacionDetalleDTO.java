package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;

// Lo que recibe Búsqueda en GET /interno/publicaciones/{id} (su clase PublicacionDetalle):
// los datos del resumen más lo que solo se ve en el detalle.
// HU-09 (CA-026 a CA-028): aquí se ven los servicios, las reglas y las imágenes.
// politicaCancelacion llega con la HU-11
public record PublicacionDetalleDTO(
        Long id,
        String titulo,
        String ciudad,
        String tipo,
        Integer capacidad,
        BigDecimal precioNoche,
        String estado,
        List<String> servicios,
        String descripcion,
        List<String> reglas,
        List<String> imagenes,
        String politicaCancelacion) {

    public static PublicacionDetalleDTO desde(Publicacion publicacion) {
        return new PublicacionDetalleDTO(
                publicacion.getId(),
                publicacion.getTitulo(),
                publicacion.getCiudad(),
                publicacion.getTipo().name(),
                publicacion.getCapacidad(),
                publicacion.getPrecioNoche(),
                publicacion.getEstado().name(),
                publicacion.getServicios(),
                publicacion.getDescripcion(),
                publicacion.getReglas(),
                publicacion.getImagenes(),
                null);
    }
}
