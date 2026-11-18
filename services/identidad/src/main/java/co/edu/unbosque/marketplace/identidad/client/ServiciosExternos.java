package co.edu.unbosque.marketplace.identidad.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

//Preguntas que este servicio le hace a los otros servicios
@Component
public class ServiciosExternos {

    private final RestClient cliente = RestClient.create();

    // Direcciones de los otros servicios, se ponen en application.properties
    @Value("${servicios.reservas.url}")
    private String urlReservas;

    @Value("${servicios.alojamientos.url}")
    private String urlAlojamientos;

    @Value("${servicios.notificaciones.url}")
    private String urlNotificaciones;
    
    @Value("${servicios.auditoria.url}")
    private String urlAuditoria;

    private static final Logger log = LoggerFactory.getLogger(ServiciosExternos.class);

    // CA-017 le pregunta a Reservas si el usuario tiene reservas futuras confirmadas
    public boolean tieneReservasFuturas(Long idUsuario) {
        // Si todavía no está el servicio de Reservas no se pregunta
        if (urlReservas.isBlank()) {
            return false;
        }
        Boolean respuesta = cliente.get()
                .uri(urlReservas + "/interno/reservas/futuras?idUsuario=" + idUsuario)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(respuesta);
    }

    // CA-018 le pide a Alojamientos que pause las publicaciones del anfitrión
    public void pausarPublicaciones(Long idAnfitrion) {
        // Si todavía no está el servicio de Alojamientos no se hace nada
        if (urlAlojamientos.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlAlojamientos + "/interno/publicaciones/pausar?idAnfitrion=" + idAnfitrion)
                .retrieve()
                .toBodilessEntity();
    }

    // HU-04 CA-010 le pide a Notificaciones que muestre el token en una notificación interna.
    // El id de la solicitud va como entidadId para que cada solicitud sea una notificación nueva
    public void notificarRecuperacion(Long idUsuario, Long idRecuperacion, String mensaje) {
        // Si todavía no está el servicio de Notificaciones no se hace nada
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
    
    // HU-35 avisa al usuario que el administrador restringió su cuenta.
    // Si Notificaciones falla, la restricción igual queda hecha
    public void notificarRestriccion(Long idUsuario) {
        notificarUsuario(idUsuario, "USUARIO_RESTRINGIDO", "Tu cuenta ha sido restringida por un administrador");
    }

    // HU-35 avisa al usuario que el administrador reactivó su cuenta
    public void notificarReactivacion(Long idUsuario) {
        notificarUsuario(idUsuario, "USUARIO_REACTIVADO", "Tu cuenta ha sido reactivada");
    }

    private void notificarUsuario(Long idUsuario, String tipoEvento, String mensaje) {
        if (urlNotificaciones.isBlank()) {
            return;
        }
        try {
            cliente.post()
                    .uri(urlNotificaciones + "/internal/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "usuarioId", idUsuario,
                            "tipoEvento", tipoEvento,
                            "entidadTipo", "USUARIO",
                            "entidadId", idUsuario,
                            "mensaje", mensaje))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo notificar al usuario {}: {}", idUsuario, e.getMessage());
        }
    }

    // HU-35 deja en Auditoría quién restringió o reactivó la cuenta.
    // Si Auditoría falla, la acción igual queda hecha
    public void registrarAuditoria(Long administradorId, Long idUsuario, String accion, String detalle) {
        if (urlAuditoria.isBlank()) {
            return;
        }
        try {
            cliente.post()
                    .uri(urlAuditoria + "/internal/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "usuarioId", administradorId,
                            "entidad", "USUARIO",
                            "entidadId", idUsuario,
                            "accion", accion,
                            "resultado", "EXITO",
                            "detalle", detalle))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo auditar la accion sobre el usuario {}: {}", idUsuario, e.getMessage());
        }
    }
}