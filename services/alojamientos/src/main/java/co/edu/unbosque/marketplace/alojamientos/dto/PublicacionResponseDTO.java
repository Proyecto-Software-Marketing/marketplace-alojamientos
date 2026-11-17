package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;

// Respuesta pública con una publicación (Listing en el contrato).
// Decisión 2: usa los nombres del contrato, que son los que espera el frontend.
// Servicios, reglas e imágenes se definen en la HU-09 y las tarifas en la HU-10 (vacías o en 0 al crear
// la publicación); motivoRechazo va en null hasta la HU-13
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
        BigDecimal tarifaLimpieza,
        Integer porcentajeDescuentoEstanciaLarga,
        Integer nochesMinimasDescuento,
        List<ServicioAdicionalDTO> serviciosAdicionales,
        List<String> servicios,
        List<String> reglas,
        List<String> imagenes,
        String estado,
        String motivoRechazo,
        String motivoBloqueo) {

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
                publicacion.getTarifaLimpieza(),
                publicacion.getPorcentajeDescuento(),
                publicacion.getNochesMinimasDescuento(),
                publicacion.getServiciosAdicionales().stream().map(ServicioAdicionalDTO::desde).toList(),
                publicacion.getServicios(),
                publicacion.getReglas(),
                publicacion.getImagenes(),
                publicacion.getEstado().name(),
                null,
                publicacion.getMotivoBloqueo());
    }
}
