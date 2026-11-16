package co.edu.unbosque.marketplace.notificaciones.client;

import co.edu.unbosque.marketplace.notificaciones.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.notificaciones.exception.AutenticacionException;
import co.edu.unbosque.marketplace.notificaciones.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

// Lo que Notificaciones necesita de otros servicios
@Component
public class ServiciosExternos {

    private static final String TODOS_LOS_ROLES = "HUESPED,ANFITRION,ADMINISTRADOR";
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

    // HU-31 (CA-087): le pregunta a Identidad de quién es el token y devuelve el id del usuario.
    // Cualquier rol con sesión puede consultar sus notificaciones.
    public Long verificarSesion(String autorizacion) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=" + TODOS_LOS_ROLES)
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);

            if (usuario == null || usuario.id() == null) {
                throw new AutenticacionException(DEBE_INICIAR_SESION);
            }
            return usuario.id();

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new AutenticacionException(DEBE_INICIAR_SESION);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde");
        }
    }

    // HU-04 (Decisión 10): le pregunta a Identidad el id del usuario dueño del correo.
    // Si el correo no está registrado devuelve vacío.
    public Optional<Long> buscarUsuarioPorCorreo(String correo) {
        try {
            UsuarioIdentidadDTO usuario = cliente.get()
                    .uri(urlIdentidad + "/interno/usuarios/id?correo={correo}", correo)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);
            return Optional.ofNullable(usuario).map(UsuarioIdentidadDTO::id);

        } catch (NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible consultar el usuario. Intente más tarde");
        }
    }
}
