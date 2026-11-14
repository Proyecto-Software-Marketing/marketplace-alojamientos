package co.edu.unbosque.marketplace.notificaciones.model;

import java.util.Arrays;
import java.util.Optional;

// Eventos que generan notificación. Cada evento sabe a qué tipo de entidad se refiere
// y qué mensaje usar si el servicio que lo origina no envía uno.
// HU-30 verifica los dos primeros; los otros se verifican en la HU-28, HU-34 y HU-35.
public enum TipoEvento {
    PUBLICACION_APROBADA(TipoEntidad.PUBLICACION, "Tu publicación fue aprobada y ya es visible para los huéspedes"),
    RESERVA_CONFIRMADA(TipoEntidad.RESERVA, "Se confirmó una reserva"),
    RESERVA_CANCELADA(TipoEntidad.RESERVA, "Se canceló una reserva"),
    PUBLICACION_BLOQUEADA(TipoEntidad.PUBLICACION, "Tu publicación fue bloqueada por un administrador"),
    USUARIO_RESTRINGIDO(TipoEntidad.USUARIO, "Tu cuenta fue restringida por un administrador");

    private final TipoEntidad entidadTipo;
    private final String mensajePorDefecto;

    TipoEvento(TipoEntidad entidadTipo, String mensajePorDefecto) {
        this.entidadTipo = entidadTipo;
        this.mensajePorDefecto = mensajePorDefecto;
    }

    // Convierte el texto recibido en el JSON (sin importar mayúsculas) o devuelve vacío si no existe
    public static Optional<TipoEvento> desde(String texto) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equalsIgnoreCase(texto.trim()))
                .findFirst();
    }

    public TipoEntidad getEntidadTipo() {
        return entidadTipo;
    }

    public String getMensajePorDefecto() {
        return mensajePorDefecto;
    }
}
