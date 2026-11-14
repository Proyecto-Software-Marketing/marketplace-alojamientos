package co.edu.unbosque.marketplace.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Cuerpo de POST /internal/notifications (NotificationCreate en el contrato).
// Lo envían Alojamientos (publicación aprobada) y Reservas (reserva confirmada), uno por usuario afectado.
public record NotificacionRequestDTO(
        @NotNull(message = "El usuario es obligatorio") Long usuarioId,
        @NotBlank(message = "El tipo de evento es obligatorio") String tipoEvento,
        String entidadTipo,
        @NotNull(message = "La entidad relacionada es obligatoria") Long entidadId,
        @Size(max = 500, message = "El mensaje no puede superar 500 caracteres") String mensaje) {
}
