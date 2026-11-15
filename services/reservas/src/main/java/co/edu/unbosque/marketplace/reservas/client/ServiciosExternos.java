package co.edu.unbosque.marketplace.reservas.client;

import co.edu.unbosque.marketplace.reservas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

// Llamadas a los otros servicios que necesita Reservas para confirmar una reserva inmediata
@Component
public class ServiciosExternos {

    private static final Logger log = LoggerFactory.getLogger(ServiciosExternos.class);
    private static final String ROLES_SESION = "HUESPED,ANFITRION";

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlDisponibilidad;
    private final String urlCotizacion;
    private final String urlNotificaciones;
    private final String urlAlojamientos;

    @Autowired
    public ServiciosExternos(
            @Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.disponibilidad.url}") String urlDisponibilidad,
            @Value("${servicios.cotizacion.url}") String urlCotizacion,
            @Value("${servicios.notificaciones.url}") String urlNotificaciones,
            @Value("${servicios.alojamientos.url}") String urlAlojamientos) {
        this(RestClient.builder(), urlIdentidad, urlDisponibilidad, urlCotizacion, urlNotificaciones, urlAlojamientos);
    }

    // Constructor para las pruebas con un servidor simulado
    ServiciosExternos(
            RestClient.Builder constructor,
            String urlIdentidad,
            String urlDisponibilidad,
            String urlCotizacion,
            String urlNotificaciones,
            String urlAlojamientos) {
        this.cliente = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlDisponibilidad = urlDisponibilidad;
        this.urlCotizacion = urlCotizacion;
        this.urlNotificaciones = urlNotificaciones;
        this.urlAlojamientos = urlAlojamientos;
    }

    // Solo un huésped puede reservar. Devuelve el id del usuario autenticado.
    public Long verificarRolHuesped(String autorizacion) {
        return verificarRol(autorizacion, "HUESPED", "Solo un huésped puede realizar una reserva").id();
    }

    // HU-26: huésped o anfitrión con sesión válida; devuelve id y rol
    public UsuarioIdentidadDTO verificarSesion(String autorizacion) {
        return verificarRol(autorizacion, ROLES_SESION, "No tiene permiso para consultar reservas");
    }

    private UsuarioIdentidadDTO verificarRol(String autorizacion, String roles, String mensajeSinPermiso) {
        if (autorizacion == null || autorizacion.isBlank()) {
            throw new AutenticacionException("Debe iniciar sesión");
        }
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=" + roles)
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);

            if (usuario == null || usuario.id() == null) {
                throw new AutenticacionException("Debe iniciar sesión");
            }
            return usuario;

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException("Debe iniciar sesión");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException(mensajeSinPermiso);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }

    // Completa los datos que no vengan en el snapshot del request (GET /quotes/{id})
    public CotizacionDTO consultarCotizacion(String cotizacionId) {
        try {
            CotizacionDTO cotizacion = cliente.get()
                    .uri(urlCotizacion + "/quotes/{id}", cotizacionId)
                    .retrieve()
                    .body(CotizacionDTO.class);

            if (cotizacion == null) {
                throw new NoEncontradoException("La cotización no existe");
            }
            return cotizacion;

        } catch (HttpClientErrorException.NotFound e) {
            throw new NoEncontradoException("La cotización no existe");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible consultar la cotización. Intente más tarde");
        }
    }

    // CA-064 y CA-065: vuelve a verificar la disponibilidad justo antes de confirmar
    public boolean verificarDisponibilidad(Long publicacionId, LocalDate fechaLlegada, LocalDate fechaSalida) {
        try {
            ConsultaDisponibilidadDTO consulta = new ConsultaDisponibilidadDTO(
                    List.of(publicacionId), fechaLlegada, fechaSalida);

            DisponibilidadResultadoDTO resultado = cliente.post()
                    .uri(urlDisponibilidad + "/internal/availability/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(consulta)
                    .retrieve()
                    .body(DisponibilidadResultadoDTO.class);

            return resultado != null
                    && resultado.disponibles() != null
                    && resultado.disponibles().contains(publicacionId);

        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible verificar la disponibilidad. Intente más tarde");
        }
    }

    // CA-066: bloquea el periodo para que ninguna otra reserva lo use
    public void bloquearPeriodo(Long publicacionId, Long reservaId, LocalDate fechaLlegada, LocalDate fechaSalida) {
        try {
            BloqueoRequestDTO solicitud = new BloqueoRequestDTO(publicacionId, reservaId, fechaLlegada, fechaSalida);

            cliente.post()
                    .uri(urlDisponibilidad + "/internal/availability/block")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Conflict e) {
            // Otra reserva ganó la carrera entre el check y el bloqueo
            throw new ConflictoReservaException("El alojamiento ya no está disponible para las fechas seleccionadas");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible bloquear el periodo. Intente más tarde");
        }
    }

    // CA-066: genera la notificación de reserva confirmada (best-effort)
    public void notificarReservaConfirmada(Long usuarioId, Long reservaId, String codigoReserva, String mensaje) {
        try {
            NotificacionRequestDTO notificacion = new NotificacionRequestDTO(
                    usuarioId,
                    "RESERVA_CONFIRMADA",
                    "RESERVA",
                    reservaId,
                    mensaje);

            cliente.post()
                    .uri(urlNotificaciones + "/internal/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notificacion)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {
            log.warn("No fue posible enviar la notificación de reserva confirmada {} al usuario {}: {}",
                    codigoReserva, usuarioId, e.getMessage());
        }
    }

    // Dueño de la publicación para notificarle (Alojamientos aún no existe: best-effort)
    public Long consultarAnfitrion(Long publicacionId) {
        try {
            PropietarioPublicacionDTO publicacion = cliente.get()
                    .uri(urlAlojamientos + "/internal/listings/{id}/owner", publicacionId)
                    .retrieve()
                    .body(PropietarioPublicacionDTO.class);

            return publicacion != null ? publicacion.anfitrionId() : null;
        } catch (RestClientException e) {
            log.debug("No se pudo obtener el anfitrión de la publicación {}: {}", publicacionId, e.getMessage());
            return null;
        }
    }
}
