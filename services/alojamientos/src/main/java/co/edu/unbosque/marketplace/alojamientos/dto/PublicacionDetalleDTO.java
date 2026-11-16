package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;

// Lo que recibe Búsqueda en GET /interno/publicaciones/{id} (su clase PublicacionDetalle):
// los datos del resumen más lo que solo se ve en el detalle.
// Decisión 13: servicios, reglas e imágenes llegan con la HU-09 y politicaCancelacion con la HU-11
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
                List.of(),
                publicacion.getDescripcion(),
                List.of(),
                List.of(),
                null);
    }
}
