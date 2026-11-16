package co.edu.unbosque.marketplace.notificaciones.client;

import co.edu.unbosque.marketplace.notificaciones.exception.AutenticacionException;
import co.edu.unbosque.marketplace.notificaciones.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Pruebas de la verificación de sesión (HU-31) con Identidad simulado
class ServiciosExternosTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_SESION =
            URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED,ANFITRION,ADMINISTRADOR";
    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String URL_POR_CORREO = URL_IDENTIDAD + "/interno/usuarios/id?correo=";

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, URL_IDENTIDAD);
    }

    @Test
    @DisplayName("CA-087 devuelve el id del usuario dueño del token")
    void devuelveUsuario() {
        // Identidad envía más campos de los que usa el DTO: se ignoran
        servidor.expect(requestTo(URL_SESION))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":21,\"rol\":\"HUESPED\",\"nombre\":\"Ana\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(21L, serviciosExternos.verificarSesion(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("401 si Identidad dice que no hay sesión")
    void sinSesion() {
        servidor.expect(requestTo(URL_SESION)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarSesion(TOKEN));
    }

    @Test
    @DisplayName("401 si Identidad rechaza el rol del usuario")
    void rolRechazado() {
        servidor.expect(requestTo(URL_SESION)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarSesion(TOKEN));
    }

    @Test
    @DisplayName("401 si Identidad responde sin id de usuario")
    void respuestaSinId() {
        servidor.expect(requestTo(URL_SESION)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarSesion(TOKEN));
    }

    @Test
    @DisplayName("503 si Identidad no responde")
    void identidadCaida() {
        servidor.expect(requestTo(URL_SESION)).andRespond(withException(new IOException("sin conexión")));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarSesion(TOKEN));
    }

    @Test
    @DisplayName("503 si Identidad tiene un error interno")
    void identidadConError() {
        servidor.expect(requestTo(URL_SESION)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarSesion(TOKEN));
    }

    // ---------- HU-04 (Decisión 10): id del usuario por correo ----------

    @Test
    @DisplayName("Decisión 10: devuelve el id del usuario dueño del correo")
    void buscaUsuarioPorCorreo() {
        // Se revisa el correo ya decodificado, sin importar cómo se codificó la @ en la URL
        servidor.expect(requestTo(startsWith(URL_POR_CORREO)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(peticion -> assertEquals("correo=ana@correo.com",
                        URLDecoder.decode(peticion.getURI().getRawQuery(), StandardCharsets.UTF_8)))
                .andRespond(withSuccess("{\"id\":21,\"rol\":\"HUESPED\",\"correo\":\"ana@correo.com\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(21L), serviciosExternos.buscarUsuarioPorCorreo("ana@correo.com"));
        servidor.verify();
    }

    @Test
    @DisplayName("Decisión 10: si el correo no está registrado devuelve vacío")
    void correoNoRegistrado() {
        servidor.expect(requestTo(startsWith(URL_POR_CORREO))).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertEquals(Optional.empty(), serviciosExternos.buscarUsuarioPorCorreo("nadie@correo.com"));
    }

    @Test
    @DisplayName("Decisión 10: 503 si Identidad no responde al buscar por correo")
    void identidadCaidaAlBuscarCorreo() {
        servidor.expect(requestTo(startsWith(URL_POR_CORREO)))
                .andRespond(withException(new IOException("sin conexión")));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.buscarUsuarioPorCorreo("ana@correo.com"));
    }
}
