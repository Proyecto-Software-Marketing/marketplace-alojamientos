package co.edu.unbosque.marketplace.resenas.dto;

import co.edu.unbosque.marketplace.resenas.model.Resena;

import java.time.LocalDateTime;

// Respuesta con una reseña (Review en el contrato)
public record ResenaResponseDTO(
        Long id,
        Long reservaId,
        Long huespedId,
        Long publicacionId,
        Integer calificacion,
        String contenido,
        Boolean visible,
        LocalDateTime fecha) {

    public static ResenaResponseDTO desde(Resena resena) {
        return new ResenaResponseDTO(
                resena.getId(),
                resena.getReservaId(),
                resena.getHuespedId(),
                resena.getPublicacionId(),
                resena.getCalificacion(),
                resena.getContenido(),
                resena.isVisible(),
                resena.getFecha());
    }
}
