package co.edu.unbosque.marketplace.disponibilidad.dto;

// Lo que responde Alojamientos en GET /internal/listings/{id}/owner (propuesta enviada a la HU-07)
public record PropietarioPublicacionDTO(Long publicacionId, Long anfitrionId, String estado) {
}