package co.edu.unbosque.marketplace.resenas.client;

import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.resenas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.PermisoException;
import co.edu.unbosque.marketplace.resenas.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Lo que Reseñas necesita de otros servicios
@Component
public class ServiciosExternos {

    private static final String DEBE_INICIAR_SESION = "Debe iniciar sesión";
    private static final String RESERVA_NO_EXISTE = "La reserva no existe";
    private static final String RESERVA_NO_DISPONIBLE = "No fue posible consultar la reserva. Intente más tarde";

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlReservas;

    @Autowired
    public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.reservas.url}") String urlReservas) {
        this(RestClient.builder(), urlIdentidad, urlReservas);
    }

    // Lo usan las pruebas para conectar un servidor simulado
    ServiciosExternos(RestClient.Builder constructor, String urlIdentidad, String urlReservas) {
        this.cliente = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlReservas = urlReservas;
    }

    // HU-32: solo un huésped puede reseñar. Devuelve el id del huésped, que sale del token
    public Long verificarHuesped(String autorizacion) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=HUESPED")
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
            throw new PermisoException("Solo un huésped puede reseñar una estancia");
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
}
