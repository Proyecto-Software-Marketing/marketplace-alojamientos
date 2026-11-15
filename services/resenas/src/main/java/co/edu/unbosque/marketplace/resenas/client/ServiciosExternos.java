package co.edu.unbosque.marketplace.resenas.client;

import co.edu.unbosque.marketplace.resenas.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.resenas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.PermisoException;
import co.edu.unbosque.marketplace.resenas.exception.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Lo que Reseñas necesita de otros servicios
@Component
public class ServiciosExternos {

    private static final Logger LOG = LoggerFactory.getLogger(ServiciosExternos.class);

    private static final String DEBE_INICIAR_SESION = "Debe iniciar sesión";
    private static final String RESERVA_NO_EXISTE = "La reserva no existe";
    private static final String RESERVA_NO_DISPONIBLE = "No fue posible consultar la reserva. Intente más tarde";

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlReservas;
    private final String urlAuditoria;

    @Autowired
    public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.reservas.url}") String urlReservas,
            @Value("${servicios.auditoria.url}") String urlAuditoria) {
        this(RestClient.builder(), urlIdentidad, urlReservas, urlAuditoria);
    }

    // Lo usan las pruebas para conectar un servidor simulado
    ServiciosExternos(RestClient.Builder constructor, String urlIdentidad, String urlReservas,
            String urlAuditoria) {
        this.cliente = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlReservas = urlReservas;
        this.urlAuditoria = urlAuditoria;
    }

    // HU-32: solo un huésped puede reseñar. Devuelve el id del huésped, que sale del token
    public Long verificarHuesped(String autorizacion) {
        return verificarRol(autorizacion, "HUESPED", "Solo un huésped puede reseñar una estancia");
    }

    // HU-36 (decisión 1): solo un administrador puede ocultar reseñas. Devuelve su id, que sale del token
    public Long verificarAdministrador(String autorizacion) {
        return verificarRol(autorizacion, "ADMINISTRADOR", "Solo un administrador puede ocultar reseñas");
    }

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

    // HU-32 (decisión 2): consulta la reserva en Reservas con el token del huésped.
    // Reservas responde 403 si la reserva es de otro usuario: se trata igual que si no existiera (404)
    public ReservaDTO consultarReserva(String autorizacion, Long reservaId) {
        try {
            ReservaDTO reserva = cliente.get()
                    .uri(urlReservas + "/bookings/{id}", reservaId)
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(ReservaDTO.class);

            if (reserva == null || reserva.huespedId() == null || reserva.publicacionId() == null
                    || reserva.estado() == null) {
                throw new ServicioNoDisponibleException(RESERVA_NO_DISPONIBLE);
            }
            return reserva;

        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            throw new NoEncontradoException(RESERVA_NO_EXISTE);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException(DEBE_INICIAR_SESION);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(RESERVA_NO_DISPONIBLE);
        }
    }

    // HU-36 (CA-101, decisión 6): envía el evento a Auditoría. Si Auditoría no responde, la operación
    // no se revierte: el registro ya quedó en la reseña y el fallo solo se anota en el log.
    // Devuelve true si Auditoría lo recibió
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
            LOG.warn("No se pudo registrar en Auditoría la acción {} sobre {} {}: {}",
                    evento.accion(), evento.entidad(), evento.entidadId(), e.getMessage());
            return false;
        }
    }
}
