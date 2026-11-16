package co.edu.unbosque.marketplace.auditoria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SanitizadorDatosSensiblesTest {

	private final SanitizadorDatosSensibles sanitizador = new SanitizadorDatosSensibles();

	@Test
	@DisplayName("CA-116: un detalle nulo no se transforma")
	void detalleNulo() {
		assertNull(sanitizador.limpiar(null));
	}

	@Test
	@DisplayName("CA-116: un detalle vacío o solo con espacios queda nulo")
	void detalleBlanco() {
		assertNull(sanitizador.limpiar("   "));
		assertNull(sanitizador.limpiar("\n\t"));
	}

	@Test
	@DisplayName("CA-116: un detalle sin datos sensibles se conserva")
	void detalleSinDatosSensibles() {
		assertEquals("Se ocultó la reseña 12 por spam",
				sanitizador.limpiar("Se ocultó la reseña 12 por spam"));
	}

	@Test
	@DisplayName("CA-116: se redacta una contraseña enviada como clave=valor")
	void redactaContrasena() {
		assertEquals("usuario=7 password=***", sanitizador.limpiar("usuario=7 password=secreto"));
		assertEquals("contraseña=***", sanitizador.limpiar("contraseña=hola"));
	}

	@Test
	@DisplayName("CA-116: se redacta una contraseña dentro de un JSON")
	void redactaContrasenaEnJson() {
		assertEquals("\"password\":***, \"usuario\":7",
				sanitizador.limpiar("\"password\":\"secreto\", \"usuario\":7"));
	}

	@Test
	@DisplayName("CA-116: se redacta el encabezado Authorization completo")
	void redactaAuthorization() {
		assertEquals("Authorization: ***, usuario=7",
				sanitizador.limpiar("Authorization: Bearer abc.def, usuario=7"));
	}

	@Test
	@DisplayName("CA-116: se redacta un token Bearer aunque no tenga clave")
	void redactaBearerSuelto() {
		assertEquals("Bearer ***", sanitizador.limpiar("Bearer abc.def"));
		assertEquals("Basic ***", sanitizador.limpiar("Basic dXNlcjpwYXNz"));
	}

	@Test
	@DisplayName("CA-116: se redacta el número de tarjeta completo, incluidos los espacios")
	void redactaTarjeta() {
		assertEquals("card=***", sanitizador.limpiar("card=4111 1111 1111 1111"));
	}

	@Test
	@DisplayName("CA-116: se redactan varios campos sensibles en el mismo detalle")
	void redactaVariosCampos() {
		assertEquals("password=***, token=***, cvv=***",
				sanitizador.limpiar("password=uno, token=dos, cvv=123"));
	}

	@Test
	@DisplayName("CA-116: los saltos de línea y tabulaciones se reemplazan por espacios")
	void reemplazaControles() {
		assertEquals("linea1 linea2 fin", sanitizador.limpiar("linea1\nlinea2\tfin"));
	}

	@Test
	@DisplayName("CA-116: el detalle se trunca para no crecer sin límite")
	void truncaDetalle() {
		String limpio = sanitizador.limpiar("a".repeat(1500));

		assertEquals(SanitizadorDatosSensibles.LONGITUD_MAXIMA_DETALLE, limpio.length());
		assertTrue(limpio.chars().allMatch(caracter -> caracter == 'a'));
	}

}
