package co.edu.unbosque.marketplace.alojamientos.dto;

import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;

// Respuesta de GET /internal/listings/{id}/owner. La leen Disponibilidad (dueño y estado) y Reservas (dueño)
public record PropietarioPublicacionDTO(Long publicacionId, Long anfitrionId, String estado) {

    public static PropietarioPublicacionDTO desde(Publicacion publicacion) {
        return new PropietarioPublicacionDTO(publicacion.getId(), publicacion.getAnfitrionId(),
                publicacion.getEstado().name());
    }
}
