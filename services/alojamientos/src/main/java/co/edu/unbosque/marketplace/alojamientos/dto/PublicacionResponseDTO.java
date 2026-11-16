package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;

// Respuesta pública con una publicación (Listing en el contrato).
// Decisión 2: usa los nombres del contrato, que son los que espera el frontend.
// Decisión 3: servicios, reglas e imágenes van como listas vacías hasta la HU-09,
// y motivoRechazo en null hasta la HU-13
public record PublicacionResponseDTO(
        Long id,
        Long anfitrionId,
        String titulo,
        String descripcion,
        String ciudad,
        String ubicacionTextual,
        String tipoAlojamiento,
        Integer capacidad,
        BigDecimal precioPorNoche,
        List<String> servicios,
        List<String> reglas,
        List<String> imagenes,
        String estado,
        String motivoRechazo) {

    public static PublicacionResponseDTO desde(Publicacion publicacion) {
        return new PublicacionResponseDTO(
                publicacion.getId(),
                publicacion.getAnfitrionId(),
                publicacion.getTitulo(),
                publicacion.getDescripcion(),
                publicacion.getCiudad(),
                publicacion.getUbicacionTextual(),
                publicacion.getTipo().name(),
                publicacion.getCapacidad(),
                publicacion.getPrecioNoche(),
                List.of(),
                List.of(),
                List.of(),
                publicacion.getEstado().name(),
                null);
    }
}
