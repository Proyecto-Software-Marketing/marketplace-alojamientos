package co.edu.unbosque.marketplace.reservas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Dueño de la publicación (lo expone Alojamientos en una ruta interna)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropietarioPublicacionDTO(Long anfitrionId) {
}
