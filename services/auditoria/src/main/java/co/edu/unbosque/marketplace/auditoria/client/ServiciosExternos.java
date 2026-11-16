package co.edu.unbosque.marketplace.auditoria.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import co.edu.unbosque.marketplace.auditoria.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.auditoria.exception.AutenticacionException;
import co.edu.unbosque.marketplace.auditoria.exception.PermisoException;
import co.edu.unbosque.marketplace.auditoria.exception.ServicioNoDisponibleException;

// Lo que Auditoria necesita de Identidad
@Component
public class ServiciosExternos {

	private final RestClient cliente;
	private final String urlIdentidad;

	@Autowired
	public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad) {
		this(RestClient.builder(), urlIdentidad);
	}

	// Constructor para las pruebas con un servidor simulado
	ServiciosExternos(RestClient.Builder constructor, String urlIdentidad) {
		this.cliente = constructor.build();
		this.urlIdentidad = urlIdentidad;
	}

	// HU-41 (CA-114): solo el administrador consulta la auditoria. Devuelve su id
	public Long verificarAdministrador(String autorizacion) {
		if (autorizacion == null || autorizacion.isBlank()) {
			throw new AutenticacionException("Debe iniciar sesion");
		}
		try {
			UsuarioIdentidadDTO usuario = cliente.get()
					.uri(urlIdentidad + "/autenticacion/autorizar?roles=ADMINISTRADOR")
					.header("Authorization", autorizacion)
					.retrieve()
					.body(UsuarioIdentidadDTO.class);

			if (usuario == null || usuario.getId() == null) {
				throw new AutenticacionException("Debe iniciar sesion");
			}
			return usuario.getId();

		} catch (HttpClientErrorException.Unauthorized e) {
			throw new AutenticacionException("Debe iniciar sesion");
		} catch (HttpClientErrorException.Forbidden e) {
			throw new PermisoException("Solo un administrador puede consultar la auditoria");
		} catch (RestClientException e) {
			throw new ServicioNoDisponibleException("No fue posible validar la sesion. Intente mas tarde");
		}
	}

}
