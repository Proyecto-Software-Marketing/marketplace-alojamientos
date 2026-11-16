package co.edu.unbosque.marketplace.reportes.dto;

import jakarta.validation.constraints.NotNull;

public record ExportRequestDTO(
        @NotNull(message = "El tipo de reporte es obligatorio") TipoReporte tipoReporte,
        @NotNull(message = "El formato del archivo es obligatorio") FormatoReporte formato) {
}
