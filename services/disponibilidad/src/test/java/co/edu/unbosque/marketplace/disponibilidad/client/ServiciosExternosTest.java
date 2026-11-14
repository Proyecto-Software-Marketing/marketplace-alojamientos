package co.edu.unbosque.marketplace.disponibilidad.client;

import co.edu.unbosque.marketplace.disponibilidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.disponibilidad.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.PermisoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Pruebas de la verificación del anfitrión dueño (HU-16) con Identidad y Alojamientos simulados
class ServiciosExternosTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_ALOJAMIENTOS = "http://alojamientos";
    private static final String URL_AUTORIZAR = URL_IDENTIDAD + "/autenticacion/autorizar?roles=ANFITRION";
    private static final String URL_DUENO = URL_ALOJAMIENTOS + "/internal/listings/15/owner";
    private static final String TOKEN = "Bearer token-de-prueba";
    private static final Long PUBLICACION = 15L;

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, URL_IDENTIDAD, URL_ALOJAMIENTOS);
    }

    // Identidad simulada: responde 200 con el usuario del token
    private void identidadResponde(String json) {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    // Alojamientos simulado: responde 200 con el dueño de la publicación
    private void alojamientosResponde(String json) {
        servidor.expect(requestTo(URL_DUENO))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("HU-16 el anfitrión dueño de la publicación puede gestionar su disponibilidad")
    void duenoAutorizado() {
        // Identidad envía más campos de los que usa el DTO: se ignoran
        identidadResponde("{\"id\":7,\"rol\":\"ANFITRION\",\"nombre\":\"Luis\",\"correo\":\"luis@correo.com\"}");
        alojamientosResponde("{\"publicacionId\":15,\"anfitrionId\":7,\"estado\":\"BORRADOR\"}");

        assertDoesNotThrow(() -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
        servidor.verify();
    }

    @Test
    @DisplayName("HU-16 un anfitrión no puede gestionar la publicación de otro anfitrión (403)")
    void otroAnfitrionNoAutorizado() {
        identidadResponde("{\"id\":7,\"rol\":\"ANFITRION\"}");
        alojamientosResponde("{\"publicacionId\":15,\"anfitrionId\":8,\"estado\":\"ACTIVA\"}");

        assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }

    @Test
    @DisplayName("Sin sesión se responde 401 sin consultar la publicación")
    void sinSesion() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
        // verify() confirma que no se hizo ninguna petición a Alojamientos
        servidor.verify();
    }

    @Test
    @DisplayName("Un usuario que no es anfitrión recibe 403")
    void rolNoPermitido() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }

    @Test
    @DisplayName("Si la publicación no existe se responde 404")
    void publicacionNoExiste() {
        identidadResponde("{\"id\":7,\"rol\":\"ANFITRION\"}");
        servidor.expect(requestTo(URL_DUENO))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }

    @Test
    @DisplayName("Si Identidad no responde no se puede verificar la sesión (503)")
    void identidadCaida() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andRespond(withException(new IOException("Sin conexión")));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }

    @Test
    @DisplayName("Si Alojamientos falla no se puede verificar al dueño (503)")
    void alojamientosConError() {
        identidadResponde("{\"id\":7,\"rol\":\"ANFITRION\"}");
        servidor.expect(requestTo(URL_DUENO))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }

    @Test
    @DisplayName("Si Identidad responde sin id de usuario se trata como sesión inválida")
    void identidadSinId() {
        identidadResponde("{}");

        assertThrows(AutenticacionException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }

    @Test
    @DisplayName("Si Alojamientos responde sin anfitrión no se puede verificar al dueño (503)")
    void alojamientosSinAnfitrion() {
        identidadResponde("{\"id\":7,\"rol\":\"ANFITRION\"}");
        alojamientosResponde("{\"publicacionId\":15}");

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.verificarDuenoPublicacion(TOKEN, PUBLICACION));
    }
}