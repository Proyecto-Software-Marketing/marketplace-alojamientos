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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

@Component
public class ServiciosExternos {

    private static final Logger log = LoggerFactory.getLogger(ServiciosExternos.class);

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

    // Constructor para pruebas unitarias con servidor simulado
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

    // Verifica que el usuario tenga rol HUESPED y devuelve su ID
    public Long verificarRolHuesped(String autorizacion) {
        if (autorizacion == null || autorizacion.isBlank()) {
            throw new AutenticacionException("Debe iniciar sesión");
        }
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=HUESPED")
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);

            if (usuario == null || usuario.id() == null) {
                throw new AutenticacionException("Debe iniciar sesión");
            }
            return usuario.id();

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException("Debe iniciar sesión");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException("Solo un huésped puede realizar una reserva");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }

    // Consulta los datos de la cotización
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

    // CA-064 y CA-065: vuelve a verificar disponibilidad con el servicio Disponibilidad
    public boolean verificarDisponibilidad(Long publicacionId, LocalDate fechaLlegada, LocalDate fechaSalida) {
        try {
            ConsultaDisponibilidadDTO solicitud = new ConsultaDisponibilidadDTO(
                    List.of(publicacionId), fechaLlegada, fechaSalida);

            DisponibilidadResultadoDTO resultado = cliente.post()
                    .uri(urlDisponibilidad + "/internal/availability/check")
                    .body(solicitud)
                    .retrieve()
                    .body(DisponibilidadResultadoDTO.class);

            return resultado != null
                    && resultado.disponibles() != null
                    && resultado.disponibles().contains(publicacionId);

        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible verificar la disponibilidad. Intente más tarde");
        }
    }

    // CA-066: bloquea el periodo en el servicio Disponibilidad (POST /internal/availability/block)
    public void bloquearPeriodo(Long publicacionId, Long reservaId, LocalDate fechaLlegada, LocalDate fechaSalida) {
        try {
            BloqueoRequestDTO solicitud = new BloqueoRequestDTO(publicacionId, reservaId, fechaLlegada, fechaSalida);

            cliente.post()
                    .uri(urlDisponibilidad + "/internal/availability/block")
                    .body(solicitud)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictoReservaException("El periodo ya no está disponible");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible bloquear el periodo. Intente más tarde");
        }
    }

    // CA-066: genera la notificación en el servicio Notificaciones (POST /internal/notifications)
    public void notificarReservaConfirmada(Long usuarioId, Long reservaId, String codigoReserva) {
        try {
            NotificacionRequestDTO notificacion = new NotificacionRequestDTO(
                    usuarioId,
                    "RESERVA_CONFIRMADA",
                    "RESERVA",
                    reservaId,
                    "Tu reserva " + codigoReserva + " ha sido confirmada"
            );

            cliente.post()
                    .uri(urlNotificaciones + "/internal/notifications")
                    .body(notificacion)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {
            log.warn("No fue posible enviar la notificación de reserva confirmada al usuario {}: {}",
                    usuarioId, e.getMessage());
        }
    }

    // Consulta el anfitrión de la publicación para enviarle notificación si es posible
    public Long consultarAnfitrion(Long publicacionId) {
        try {
            PropietarioPublicacionDTO publicacion = cliente.get()
                    .uri(urlAlojamientos + "/internal/listings/{id}/owner", publicacionId)
                    .retrieve()
                    .body(PropietarioPublicacionDTO.class);

            return publicacion != null ? publicacion.anfitrionId() : null;
        } catch (Exception e) {
            log.debug("No se pudo obtener el dueño de la publicación {}: {}", publicacionId, e.getMessage());
            return null;
        }
    }
}
