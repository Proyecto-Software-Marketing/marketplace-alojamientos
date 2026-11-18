package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

import java.math.BigDecimal;
import java.util.List;


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
        Long politicaCancelacionId) {

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
                publicacion.getMotivoRechazo(),
                publicacion.getPoliticaCancelacion() == null
                        ? null
                        : publicacion.getPoliticaCancelacion().getId());
    }
}