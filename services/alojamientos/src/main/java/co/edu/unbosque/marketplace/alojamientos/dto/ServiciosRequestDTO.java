package co.edu.unbosque.marketplace.alojamientos.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// Cuerpo de PUT /listings/{id}/amenities (AmenitiesInput en el contrato). Reemplaza la lista completa
public record ServiciosRequestDTO(
        @NotNull(message = "La lista de servicios es obligatoria")
        List<String> servicios) {
}
