
package co.edu.unbosque.marketplace.alojamientos.client;
import co.edu.unbosque.marketplace.alojamientos.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.AutenticacionException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.exception.ServicioNoDisponibleException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AuditoriaEdicionTest {

    private static final String URL_AUDITORIA = "http://auditoria";
    private static final String URL_EVENTOS =
            URL_AUDITORIA + "/internal/audit-events";

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();

        serviciosExternos = new ServiciosExternos(
                constructor, "http://identidad", URL_AUDITORIA);
    }

    @Test
    void ca024EnviaEventoCorrectoAAuditoria() {
        servidor.expect(requestTo(URL_EVENTOS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "usuarioId": 10,
                          "entidad": "PUBLICACION",
                          "entidadId": 25,
                          "accion": "EDITAR_PUBLICACION",
                          "resultado": "EXITO",
                          "detalle": "El administrador actualizo los datos de la publicacion"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertTrue(serviciosExternos.registrarAuditoriaEdicion(10L, 25L));
        servidor.verify();
    }

    @Test
    void ca024AuditoriaNoDisponibleDevuelveFalse() {
        servidor.expect(requestTo(URL_EVENTOS))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(
                        new IOException("Auditoria no disponible")));

        assertFalse(serviciosExternos.registrarAuditoriaEdicion(10L, 25L));
        servidor.verify();
    }

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String URL_EDITOR =
            "http://identidad/autenticacion/autorizar?roles=ANFITRION,ADMINISTRADOR";

    @Test
    void editorAnfitrionAutorizado() {
        servidor.expect(requestTo(URL_EDITOR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess(
                        "{\"id\":7,\"rol\":\"ANFITRION\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        UsuarioIdentidadDTO usuario = serviciosExternos.verificarEditor(TOKEN);

        assertEquals(7L, usuario.id());
        assertEquals("ANFITRION", usuario.rol());
        servidor.verify();
    }

    @Test
    void editorAdministradorAutorizado() {
        servidor.expect(requestTo(URL_EDITOR))
                .andRespond(withSuccess(
                        "{\"id\":10,\"rol\":\"ADMINISTRADOR\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        UsuarioIdentidadDTO usuario = serviciosExternos.verificarEditor(TOKEN);

        assertEquals(10L, usuario.id());
        assertEquals("ADMINISTRADOR", usuario.rol());
        servidor.verify();
    }

    @Test
    void editorSinSesionRecibe401() {
        servidor.expect(requestTo(URL_EDITOR))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class,
                () -> serviciosExternos.verificarEditor(TOKEN));
        servidor.verify();
    }

    @Test
    void editorSinPermisoRecibe403() {
        servidor.expect(requestTo(URL_EDITOR))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarEditor(TOKEN));
        servidor.verify();
    }

    @Test
    void editorIdentidadSinDatosRecibe401() {
        servidor.expect(requestTo(URL_EDITOR))
                .andRespond(withSuccess(
                        "{}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        assertThrows(AutenticacionException.class,
                () -> serviciosExternos.verificarEditor(TOKEN));
        servidor.verify();
    }

    @Test
    void editorIdentidadNoDisponibleRecibe503() {
        servidor.expect(requestTo(URL_EDITOR))
                .andRespond(withException(
                        new IOException("Identidad no disponible")));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.verificarEditor(TOKEN));
        servidor.verify();
    }

}
