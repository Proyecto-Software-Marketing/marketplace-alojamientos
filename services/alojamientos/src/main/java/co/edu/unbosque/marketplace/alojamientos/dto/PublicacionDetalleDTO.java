package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;


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
         String politicaCancelacion,
        List<ServicioAdicionalDTO> serviciosAdicionales) {

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
                publicacion.getPoliticaCancelacion() == null ? null : publicacion.getPoliticaCancelacion().getNombre(),
                publicacion.getServiciosAdicionales().stream().map(ServicioAdicionalDTO::desde).toList());
    }
}
