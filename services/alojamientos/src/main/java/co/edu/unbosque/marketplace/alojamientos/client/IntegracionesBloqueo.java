package co.edu.unbosque.marketplace.alojamientos.client;

import co.edu.unbosque.marketplace.alojamientos.dto.EventoAuditoriaBloqueoDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.NotificacionBloqueoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class IntegracionesBloqueo {

    private static final Logger log =
            LoggerFactory.getLogger(IntegracionesBloqueo.class);

    private final RestClient cliente;
    private final String urlReservas;
    private final String urlNotificaciones;
    private final String urlAuditoria;

    @Autowired
    public IntegracionesBloqueo(
            @Value("${servicios.reservas.url}") String urlReservas,
            @Value("${servicios.notificaciones.url}") String urlNotificaciones,
            @Value("${servicios.auditoria.url}") String urlAuditoria) {

        this(RestClient.builder(), urlReservas, urlNotificaciones, urlAuditoria);
    }

    // Constructor utilizado por las pruebas HTTP.
    IntegracionesBloqueo(
            RestClient.Builder constructor,
            String urlReservas,
            String urlNotificaciones,
            String urlAuditoria) {

        this.cliente = constructor.build();
        this.urlReservas = urlReservas;
        this.urlNotificaciones = urlNotificaciones;
        this.urlAuditoria = urlAuditoria;
    }

    // CA-096: avisar al anfitrion del bloqueo.
    public boolean notificarBloqueo(
            Long anfitrionId, Long publicacionId, String motivo) {

        NotificacionBloqueoDTO solicitud = new NotificacionBloqueoDTO(
                anfitrionId,
                "PUBLICACION_BLOQUEADA",
                "PUBLICACION",
                publicacionId,
                construirMensaje(motivo));

        try {
            cliente.post()
                    .uri(urlNotificaciones + "/internal/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (RestClientException e) {
            log.warn("No se pudo notificar el bloqueo de la publicacion {}",
                    publicacionId, e);
            return false;
        }
    }

    // CA-096: registrar quien bloqueo la publicacion y por que.
    public boolean registrarAuditoria(
            Long administradorId, Long publicacionId, String motivo) {

        EventoAuditoriaBloqueoDTO evento = new EventoAuditoriaBloqueoDTO(
                administradorId,
                "PUBLICACION",
                publicacionId,
                "BLOQUEAR_PUBLICACION",
                "EXITO",
                motivo);

        try {
            cliente.post()
                    .uri(urlAuditoria + "/internal/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (RestClientException e) {
            log.warn("No se pudo auditar el bloqueo de la publicacion {}",
                    publicacionId, e);
            return false;
        }
    }

    private String construirMensaje(String motivo) {
        String prefijo = "Tu publicacion fue bloqueada por un administrador. Motivo: ";
        String texto = motivo == null ? "" : motivo;
        int maximoMotivo = 500 - prefijo.length();

        if (texto.length() > maximoMotivo) {
            texto = texto.substring(0, maximoMotivo);
        }

        return prefijo + texto;
    }
    // CA-095: solicitar revision manual de reservas futuras confirmadas.
    public boolean solicitarRevisionManual(Long publicacionId) {
        try {
            cliente.post()
                    .uri(urlReservas + "/internal/bookings/review-manual")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("publicacionId", publicacionId))
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (RestClientException e) {
            log.warn("No se pudo solicitar revision manual para la publicacion {}",
                    publicacionId, e);
            return false;
        }
    }}