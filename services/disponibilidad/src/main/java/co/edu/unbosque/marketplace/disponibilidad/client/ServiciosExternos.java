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

    private static final String TODOS_LOS_ROLES = "HUESPED,ANFITRION,ADMINISTRADOR";
    private static final String PUBLICACION_NO_DISPONIBLE =
            "No fue posible consultar la publicación. Intente más tarde";

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
        Long idUsuario = verificarRol(autorizacion, "ANFITRION",
                "Solo un anfitrión puede gestionar la disponibilidad");
        Long idAnfitrion = consultarPublicacion(idPublicacion).anfitrionId();

        if (idAnfitrion == null) {
            throw new ServicioNoDisponibleException(PUBLICACION_NO_DISPONIBLE);
        }
        if (!idAnfitrion.equals(idUsuario)) {
            throw new PermisoException(
                    "Solo el anfitrión dueño de la publicación puede gestionar su disponibilidad");
        }
    }

    // HU-17 (decisión 2): cualquier usuario con sesión puede consultar el calendario
    public void verificarSesion(String autorizacion) {
        verificarRol(autorizacion, TODOS_LOS_ROLES, "No tiene permiso para consultar el calendario");
    }

    // HU-17 (decisión 1): el estado dice si un administrador bloqueó la publicación
    public String consultarEstadoPublicacion(Long idPublicacion) {
        String estado = consultarPublicacion(idPublicacion).estado();
        if (estado == null) {
            throw new ServicioNoDisponibleException(PUBLICACION_NO_DISPONIBLE);
        }
        return estado;
    }

    // Le pregunta a Identidad si el token es de alguno de los roles y devuelve el id del usuario
    private Long verificarRol(String autorizacion, String roles, String mensajeSinPermiso) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=" + roles)
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
            throw new PermisoException(mensajeSinPermiso);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }

    // Le pregunta a Alojamientos por la publicación (GET /internal/listings/{id}/owner)
    private PropietarioPublicacionDTO consultarPublicacion(Long idPublicacion) {
        try {
            PropietarioPublicacionDTO publicacion = cliente.get()
                    .uri(urlAlojamientos + "/internal/listings/{id}/owner", idPublicacion)
                    .retrieve()
                    .body(PropietarioPublicacionDTO.class);

            if (publicacion == null) {
                throw new ServicioNoDisponibleException(PUBLICACION_NO_DISPONIBLE);
            }
            return publicacion;

        } catch (HttpClientErrorException.NotFound e) {
            throw new NoEncontradoException("La publicación no existe");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(PUBLICACION_NO_DISPONIBLE);
        }
    }
}