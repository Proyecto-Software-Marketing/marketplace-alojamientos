package co.edu.unbosque.marketplace.alojamientos.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// Cuerpo de PUT /listings/{id}/rules (RulesInput en el contrato). Reemplaza la lista completa
public record ReglasRequestDTO(
        @NotNull(message = "La lista de reglas es obligatoria")
        List<String> reglas) {
}
