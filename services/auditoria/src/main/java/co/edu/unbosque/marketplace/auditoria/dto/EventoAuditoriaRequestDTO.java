package co.edu.unbosque.marketplace.auditoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EventoAuditoriaRequestDTO(
		@Positive(message = "El usuario debe ser válido") Long usuarioId,

		@NotBlank(message = "La entidad es obligatoria")
		@Size(max = 40, message = "La entidad no puede superar 40 caracteres") String entidad,

		@Positive(message = "La entidad relacionada debe ser válida") Long entidadId,

		@NotBlank(message = "La acción es obligatoria")
		@Size(max = 60, message = "La acción no puede superar 60 caracteres") String accion,

		@NotBlank(message = "El resultado es obligatorio")
		@Size(max = 30, message = "El resultado no puede superar 30 caracteres") String resultado,

		@Size(max = 2000, message = "El detalle no puede superar 2000 caracteres") String detalle) {
}
