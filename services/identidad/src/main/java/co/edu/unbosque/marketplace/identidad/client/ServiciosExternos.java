package co.edu.unbosque.marketplace.identidad.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ServiciosExternos {

    private final RestClient cliente = RestClient.create();

    @Value("${servicios.reservas.url}")
    private String urlReservas;

    @Value("${servicios.alojamientos.url}")
    private String urlAlojamientos;

    @Value("${servicios.notificaciones.url}")
    private String urlNotificaciones;

    @Value("${servicios.auditoria.url}")
    private String urlAuditoria;

    public boolean tieneReservasFuturas(Long idUsuario) {
        if (urlReservas.isBlank()) {
            return false;
        }
        Boolean respuesta = cliente.get()
                .uri(urlReservas + "/interno/reservas/futuras?idUsuario=" + idUsuario)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(respuesta);
    }

    public void pausarPublicaciones(Long idAnfitrion) {
        if (urlAlojamientos.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlAlojamientos + "/interno/publicaciones/pausar?idAnfitrion=" + idAnfitrion)
                .retrieve()
                .toBodilessEntity();
    }

    public void notificarRecuperacion(Long idUsuario, Long idRecuperacion, String mensaje) {
        if (urlNotificaciones.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlNotificaciones + "/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "usuarioId", idUsuario,
                        "tipoEvento", "RECUPERACION_CONTRASENA",
                        "entidadTipo", "USUARIO",
                        "entidadId", idRecuperacion,
                        "mensaje", mensaje))
                .retrieve()
                .toBodilessEntity();
    }

    public void notificarRestriccion(Long idUsuario) {
        if (urlNotificaciones.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlNotificaciones + "/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "usuarioId", idUsuario,
                        "tipoEvento", "USUARIO_RESTRINGIDO",
                        "entidadTipo", "USUARIO",
                        "entidadId", idUsuario,
                        "mensaje", "Tu cuenta ha sido restringida por un administrador"))
                .retrieve()
                .toBodilessEntity();
    }

    public void notificarReactivacion(Long idUsuario) {
        if (urlNotificaciones.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlNotificaciones + "/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "usuarioId", idUsuario,
                        "tipoEvento", "USUARIO_REACTIVADO",
                        "entidadTipo", "USUARIO",
                        "entidadId", idUsuario,
                        "mensaje", "Tu cuenta ha sido reactivada"))
                .retrieve()
                .toBodilessEntity();
    }

    public void registrarAuditoria(Long administradorId, Long idUsuario,
            String accion, String detalle) {
        if (urlAuditoria.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlAuditoria + "/internal/audit-events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "usuarioId", administradorId,
                        "entidad", "USUARIO",
                        "entidadId", idUsuario,
                        "accion", accion,
                        "resultado", "EXITOSO",
                        "detalle", detalle))
                .retrieve()
                .toBodilessEntity();
    }
}