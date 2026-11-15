package co.edu.unbosque.marketplace.notificaciones.dto;

import java.util.List;

// Respuesta de GET /notifications (NotificationList en el contrato).
// CA-085: items de la más reciente a la más antigua; cada una trae "leida" y
// noLeidas sirve para el contador de la campana en el frontend.
public record NotificacionListaDTO(List<NotificacionResponseDTO> items, long noLeidas) {
}
