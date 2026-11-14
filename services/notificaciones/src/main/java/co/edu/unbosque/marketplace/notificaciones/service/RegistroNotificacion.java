package co.edu.unbosque.marketplace.notificaciones.service;

import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;

// Resultado de registrar: la notificación y si es nueva o ya existía (reintento, decisión 3)
public record RegistroNotificacion(Notificacion notificacion, boolean nueva) {
}
