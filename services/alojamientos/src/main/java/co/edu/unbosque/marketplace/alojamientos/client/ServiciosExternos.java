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

// Lo que Alojamientos necesita de otros servicios
@Component
public class ServiciosExternos {

    private static final String DEBE_INICIAR_SESION = "Debe iniciar sesión";

    private final RestClient cliente;
    private final String urlIdentidad;

    @Autowired
    public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad) {
        this(RestClient.builder(), urlIdentidad);
    }

    // Lo usan las pruebas para conectar un servidor simulado
    ServiciosExternos(RestClient.Builder constructor, String urlIdentidad) {
        this.cliente = constructor.build();
        this.urlIdentidad = urlIdentidad;
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


    // HU-34 (CA-094): solo un administrador puede bloquear publicaciones.
    public Long verificarAdministrador(String autorizacion) {
        return verificarRol(
                autorizacion,
                "ADMINISTRADOR",
                "Solo un administrador puede bloquear publicaciones");
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
}
