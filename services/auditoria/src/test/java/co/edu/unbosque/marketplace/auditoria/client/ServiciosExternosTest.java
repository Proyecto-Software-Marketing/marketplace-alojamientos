package co.edu.unbosque.marketplace.auditoria.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.auditoria.exception.AutenticacionException;
import co.edu.unbosque.marketplace.auditoria.exception.PermisoException;
import co.edu.unbosque.marketplace.auditoria.exception.ServicioNoDisponibleException;

// Pruebas de la llamada a Identidad para validar el rol del administrador
class ServiciosExternosTest {

	private static final String URL_AUTORIZAR = "http://identidad/autenticacion/autorizar?roles=ADMINISTRADOR";
	private static final String TOKEN = "Bearer token-de-prueba";

	private MockRestServiceServer servidor;
	private ServiciosExternos serviciosExternos;

	@BeforeEach
	void preparar() {
		RestClient.Builder constructor = RestClient.builder();
		servidor = MockRestServiceServer.bindTo(constructor).build();
		serviciosExternos = new ServiciosExternos(constructor, "http://identidad");
	}

	@Test
	@DisplayName("CA-114: el administrador queda identificado con su id")
	void elAdministradorPasa() {
		servidor.expect(requestTo(URL_AUTORIZAR))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", TOKEN))
				.andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));

		assertEquals(7L, serviciosExternos.verificarAdministrador(TOKEN));
		servidor.verify();
	}

	@Test
	@DisplayName("Sin sesion valida da 401")
	void sinSesionValidaDa401() {
		servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAdministrador(TOKEN));
	}

	@Test
	@DisplayName("Si no es administrador da 403")
	void siNoEsAdministradorDa403() {
		servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThrows(PermisoException.class, () -> serviciosExternos.verificarAdministrador(TOKEN));
	}

	@Test
	@DisplayName("Sin token ni siquiera pregunta a Identidad")
	void sinTokenNoPreguntaAIdentidad() {
		assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAdministrador(null));
		assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAdministrador(" "));
	}

	@Test
	@DisplayName("Si Identidad no responde se avisa con 503")
	void identidadNoResponde() {
		servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarAdministrador(TOKEN));
	}

}
