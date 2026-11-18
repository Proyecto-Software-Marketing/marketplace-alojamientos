package co.edu.unbosque.marketplace.alojamientos.client;

import co.edu.unbosque.marketplace.alojamientos.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.AutenticacionException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import co.edu.unbosque.marketplace.alojamientos.dto.EventoAuditoriaDTO;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unbosque.marketplace.alojamientos.dto.NotificacionRequestDTO;

// Lo que Alojamientos necesita de otros servicios
@Component
public class ServiciosExternos {

    private static final String DEBE_INICIAR_SESION = "Debe iniciar sesión";
    private static final Logger log = LoggerFactory.getLogger(ServiciosExternos.class);

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlAuditoria;
    private final String urlNotificaciones;

   @Autowired
public ServiciosExternos(
        @Value("${servicios.identidad.url}") String urlIdentidad,
        @Value("${servicios.auditoria.url}") String urlAuditoria,
        @Value("${servicios.notificaciones.url}") String urlNotificaciones) {
    this(RestClient.builder(), urlIdentidad, urlAuditoria, urlNotificaciones);
}

// Conservamos este constructor para las pruebas existentes
ServiciosExternos(RestClient.Builder constructor, String urlIdentidad) {
    this(constructor, urlIdentidad, "http://localhost:8090", "http://localhost:8088");
}

ServiciosExternos(
        RestClient.Builder constructor,
        String urlIdentidad,
        String urlAuditoria,
        String urlNotificaciones) {
    this.cliente = constructor.build();
    this.urlIdentidad = urlIdentidad;
    this.urlAuditoria = urlAuditoria;
    this.urlNotificaciones = urlNotificaciones;
}

    // HU-07 (CA-022, decisión 4): solo un anfitrión puede crear publicaciones.
    // Devuelve el id del anfitrión, que sale del token
    public Long verificarAnfitrion(String autorizacion) {
        return verificarAnfitrion(autorizacion, "Solo un anfitrión puede crear publicaciones");
    }

    // Igual que el anterior, con el mensaje de la operación (HU-09: modificar la publicación)
    public Long verificarAnfitrion(String autorizacion, String mensajeSinPermiso) {
        return verificarRol(autorizacion, "ANFITRION", mensajeSinPermiso);
    }


    // HU-08: permite consultar publicaciones al administrador o al anfitrion propietario.
    public Long verificarAdministradorOAnfitrion(String autorizacion, Long propietarioId) {
        try {
            return verificarAdministrador(autorizacion);
        } catch (PermisoException e) {
            Long anfitrionId = verificarAnfitrion(
                    autorizacion,
                    "No tiene permisos para consultar esta publicacion");

            if (!anfitrionId.equals(propietarioId)) {
                throw new PermisoException(
                        "No puede consultar una publicacion de otro anfitrion");
            }

            return anfitrionId;
        }
    }
    // HU-13: solo un administrador puede aprobar o rechazar publicaciones.
    public Long verificarAdministrador(String autorizacion) {
        return verificarRol(
                autorizacion,
                "ADMINISTRADOR",
                "Solo un administrador puede aprobar o rechazar publicaciones");
    }

    // Le pregunta a Identidad si el token es del rol y devuelve el id del usuario
    private Long verificarRol(String autorizacion, String rol, String mensajeSinPermiso) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=" + rol)
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);

            if (usuario == null || usuario.id() == null) {
                throw new AutenticacionException(DEBE_INICIAR_SESION);
            }
            return usuario.id();

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException(DEBE_INICIAR_SESION);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException(mensajeSinPermiso);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }
    // HU-13 (CA-039): registra las decisiones del administrador.
public boolean registrarAuditoria(EventoAuditoriaDTO evento) {
    try {
        cliente.post()
                .uri(urlAuditoria + "/internal/audit-events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(evento)
                .retrieve()
                .toBodilessEntity();

        return true;

    } catch (RestClientException e) {
        log.warn("No fue posible registrar la auditoria de la publicacion {}",
                evento.entidadId(), e);
        return false;
    }
}
// HU-13 (CA-039): notifica al anfitrion cuando aprueban su publicacion.
public boolean notificarPublicacionAprobada(Long anfitrionId, Long publicacionId) {
    try {
        NotificacionRequestDTO notificacion = new NotificacionRequestDTO(
                anfitrionId,
                "PUBLICACION_APROBADA",
                "PUBLICACION",
                publicacionId,
                "Tu publicacion ha sido aprobada y ahora esta activa."
        );

        cliente.post()
                .uri(urlNotificaciones + "/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(notificacion)
                .retrieve()
                .toBodilessEntity();

        return true;

    } catch (RestClientException e) {
        log.warn("No fue posible notificar la aprobacion de la publicacion {}",
                publicacionId, e);
        return false;
    }
}
// HU-13 (CA-039): notifica al anfitrion cuando rechazan su publicacion.
public boolean notificarPublicacionRechazada(
        Long anfitrionId, Long publicacionId, String motivo) {
    try {
        NotificacionRequestDTO notificacion = new NotificacionRequestDTO(
                anfitrionId,
                "PUBLICACION_RECHAZADA",
                "PUBLICACION",
                publicacionId,
                "Tu publicacion fue rechazada. Motivo: " + motivo
        );

        cliente.post()
                .uri(urlNotificaciones + "/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(notificacion)
                .retrieve()
                .toBodilessEntity();

        return true;

    } catch (RestClientException e) {
        log.warn("No fue posible notificar el rechazo de la publicacion {}",
                publicacionId, e);
        return false;
    }
}
}
