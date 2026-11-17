package co.edu.unbosque.marketplace.alojamientos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unbosque.marketplace.alojamientos.dto.EventoAuditoriaDTO;
import org.springframework.http.MediaType;
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

// Lo que Alojamientos necesita de otros servicios
@Component
public class ServiciosExternos {
    private static final Logger LOG =
        LoggerFactory.getLogger(ServiciosExternos.class);

    private static final String DEBE_INICIAR_SESION = "Debe iniciar sesión";

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlAuditoria;

@Autowired
public ServiciosExternos(
        @Value("${servicios.identidad.url}") String urlIdentidad,
        @Value("${servicios.auditoria.url}") String urlAuditoria) {
    this(RestClient.builder(), urlIdentidad, urlAuditoria);
}

 // Conserva la compatibilidad con las pruebas existentes.
ServiciosExternos(RestClient.Builder constructor, String urlIdentidad) {
    this(constructor, urlIdentidad, "http://localhost:8090");
}

ServiciosExternos(RestClient.Builder constructor,
        String urlIdentidad, String urlAuditoria) {
    this.cliente = constructor.build();
    this.urlIdentidad = urlIdentidad;
    this.urlAuditoria = urlAuditoria;
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

    // HU-08: autoriza a un anfitrion o administrador para editar.
    public UsuarioIdentidadDTO verificarEditor(String autorizacion) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=ANFITRION,ADMINISTRADOR")
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);

            if (usuario == null || usuario.id() == null || usuario.rol() == null) {
                throw new AutenticacionException("Debe iniciar sesión");
            }

            return usuario;
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException("Debe iniciar sesión");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException("No tiene permiso para editar publicaciones");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(
                    "No fue posible validar la sesión. Intente más tarde");
        }
    }


    // HU-08 CA-024: registra las ediciones realizadas por administradores.
    // Si Auditoria falla, la edicion guardada no se revierte.
    public boolean registrarAuditoriaEdicion(Long administradorId, Long publicacionId) {
        EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                administradorId,
                "PUBLICACION",
                publicacionId,
                "EDITAR_PUBLICACION",
                "EXITO",
                "El administrador actualizo los datos de la publicacion");

        try {
            cliente.post()
                    .uri(urlAuditoria + "/internal/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (RestClientException e) {
            LOG.warn(
                    "No se pudo registrar en Auditoria la edicion de la publicacion {}: {}",
                    publicacionId, e.getMessage());
            return false;
        }
    }

}
