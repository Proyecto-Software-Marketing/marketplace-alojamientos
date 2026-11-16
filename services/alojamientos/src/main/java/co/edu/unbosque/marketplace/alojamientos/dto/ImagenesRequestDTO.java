package co.edu.unbosque.marketplace.alojamientos.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// Cuerpo de PUT /listings/{id}/images (ImagesInput en el contrato). Reemplaza la lista completa
public record ImagenesRequestDTO(
        @NotNull(message = "La lista de imágenes es obligatoria")
        List<String> imagenes) {
}
