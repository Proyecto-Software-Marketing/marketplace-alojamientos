package co.edu.unbosque.marketplace.resenas.dto;

import java.util.List;

// Respuesta de GET /listings/{id}/reviews (ReviewList en el contrato).
// Decisión 3: calificacionPromedio es null si la publicación no tiene reseñas visibles
public record ResenaListaDTO(Double calificacionPromedio, List<ResenaResponseDTO> items) {
}
