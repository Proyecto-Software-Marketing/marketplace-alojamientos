package co.edu.unbosque.marketplace.disponibilidad.client;

import co.edu.unbosque.marketplace.disponibilidad.dto.PropietarioPublicacionDTO;
import co.edu.unbosque.marketplace.disponibilidad.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.disponibilidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.disponibilidad.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.PermisoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Lo que Disponibilidad necesita de otros servicios
@Component
public class ServiciosExternos {

    private final RestClient cliente;
    private final String urlIdentidad;
    private final String urlAlojamientos;

    @Autowired
    public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.alojamientos.url}") String urlAlojamientos) {
        this(RestClient.builder(), urlIdentidad, urlAlojamientos);
    }

    // Lo usan las pruebas para conectar un servidor simulado
    ServiciosExternos(RestClient.Builder constructor, String urlIdentidad, String urlAlojamientos) {
        this.cliente = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlAlojamientos = urlAlojamientos;
    }

    // HU-16: solo el anfitrión dueño de la publicación gestiona su disponibilidad
    public void verificarDuenoPublicacion(String autorizacion, Long idPublicacion) {
        Long idUsuario = verificarAnfitrion(autorizacion);
        Long idAnfitrion = consultarAnfitrion(idPublicacion);

        if (!idAnfitrion.equals(idUsuario)) {
            throw new PermisoException(
                    "Solo el anfitrión dueño de la publicación puede gestionar su disponibilidad");
        }
    }

    // Le pregunta a Identidad si el token es de un ANFITRION y devuelve su id
    private Long verificarAnfitrion(String autorizacion) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=ANFITRION")
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
            throw new PermisoException("Solo un anfitrión puede gestionar la disponibilidad");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }

    // Le pregunta a Alojamientos quién es el anfitrión de la publicación
    private Long consultarAnfitrion(Long idPublicacion) {
        try {
            PropietarioPublicacionDTO propietario = cliente.get()
                    .uri(urlAlojamientos + "/internal/listings/{id}/owner", idPublicacion)
                    .retrieve()
                    .body(PropietarioPublicacionDTO.class);

            if (propietario == null || propietario.anfitrionId() == null) {
                throw new ServicioNoDisponibleException("No fue posible consultar la publicación. Intente más tarde");
            }
            return propietario.anfitrionId();

        } catch (HttpClientErrorException.NotFound e) {
            throw new NoEncontradoException("La publicación no existe");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible consultar la publicación. Intente más tarde");
        }
    }
}