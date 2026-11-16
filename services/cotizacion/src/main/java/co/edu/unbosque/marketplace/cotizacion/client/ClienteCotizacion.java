package co.edu.unbosque.marketplace.cotizacion.client;

import co.edu.unbosque.marketplace.cotizacion.dto.DatosPublicacion;
import co.edu.unbosque.marketplace.cotizacion.dto.UsuarioIdentidad;
import co.edu.unbosque.marketplace.cotizacion.exception.AutenticacionException;
import co.edu.unbosque.marketplace.cotizacion.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.cotizacion.exception.PermisoException;
import co.edu.unbosque.marketplace.cotizacion.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// HU-22: lo que la cotización necesita de Identidad y de Alojamientos
@Component
public class ClienteCotizacion {

    private static final String DEBE_INICIAR_SESION = "Debe iniciar sesión";
    private static final String MENSAJE_ALOJAMIENTOS =
            "No fue posible consultar la publicación. Intente más tarde";

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlAlojamientos;

    @Autowired
    public ClienteCotizacion(@Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.alojamientos.url}") String urlAlojamientos) {
        this(RestClient.builder(), urlIdentidad, urlAlojamientos);
    }

    // Lo usan las pruebas para conectar un servidor simulado
    ClienteCotizacion(RestClient.Builder constructor, String urlIdentidad, String urlAlojamientos) {
        this.cliente = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlAlojamientos = urlAlojamientos;
    }

    // Decisión 1: solo un huésped puede cotizar. Devuelve su id, que sale del token
    public Long verificarHuesped(String autorizacion) {
        try {
            UsuarioIdentidad usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=HUESPED")
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidad.class);

            if (usuario == null || usuario.getId() == null) {
                throw new AutenticacionException(DEBE_INICIAR_SESION);
            }
            return usuario.getId();

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException(DEBE_INICIAR_SESION);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException("Solo un huésped puede pedir una cotización");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }

    // Decisión 5: precio, capacidad, limpieza, descuento y servicios adicionales de la publicación (HU-10)
    public DatosPublicacion consultarDatosPublicacion(Long publicacionId) {
        try {
            DatosPublicacion datos = cliente.get()
                    .uri(urlAlojamientos + "/internal/listings/{id}/quote-data", publicacionId)
                    .retrieve()
                    .body(DatosPublicacion.class);

            if (datos == null) {
                throw new ServicioNoDisponibleException(MENSAJE_ALOJAMIENTOS);
            }
            return datos;

        } catch (HttpClientErrorException.NotFound e) {
            throw new NoEncontradoException("La publicación no existe");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(MENSAJE_ALOJAMIENTOS);
        }
    }
}
