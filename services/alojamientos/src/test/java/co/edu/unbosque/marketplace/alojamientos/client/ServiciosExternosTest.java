package co.edu.unbosque.marketplace.alojamientos.client;

import co.edu.unbosque.marketplace.alojamientos.exception.AutenticacionException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import co.edu.unbosque.marketplace.alojamientos.dto.EventoAuditoriaDTO;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

// Pruebas de la HU-07 con Identidad simulada
class ServiciosExternosTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_AUTORIZAR = URL_IDENTIDAD + "/autenticacion/autorizar?roles=ANFITRION";
    private static final String TOKEN = "Bearer token-de-prueba";

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, URL_IDENTIDAD);
    }

    @Test
    @DisplayName("CA-020 el anfitrión con sesión obtiene su id desde el token")
    void anfitrionConSesion() {
        // Identidad envía más campos de los que usa el DTO: se ignoran
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":7,\"rol\":\"ANFITRION\",\"nombre\":\"Luis\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(7L, serviciosExternos.verificarAnfitrion(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-022 un huésped recibe 403 de Identidad y no puede crear publicaciones")
    void noEsAnfitrion() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        PermisoException error = assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarAnfitrion(TOKEN));
        assertEquals("Solo un anfitrión puede crear publicaciones", error.getMessage());
    }

    @Test
    @DisplayName("HU-09: al modificar la publicación, el huésped recibe el mensaje de esa operación")
    void noEsAnfitrionAlModificar() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        PermisoException error = assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarAnfitrion(TOKEN, "Solo el anfitrión puede modificar sus publicaciones"));
        assertEquals("Solo el anfitrión puede modificar sus publicaciones", error.getMessage());
    }

    @Test
    @DisplayName("HU-09: al modificar la publicación, el anfitrión obtiene su id desde el token")
    void anfitrionAlModificar() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andRespond(withSuccess("{\"id\":7,\"rol\":\"ANFITRION\"}", MediaType.APPLICATION_JSON));

        assertEquals(7L, serviciosExternos.verificarAnfitrion(TOKEN, "Sin permiso"));
    }

    @Test
    @DisplayName("Decisión 4: sin sesión o con el token vencido se responde 401")
    void sinSesion() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAnfitrion(TOKEN));
    }

    @Test
    @DisplayName("Decisión 4: si Identidad responde sin id de usuario se trata como sesión inválida")
    void identidadSinId() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAnfitrion(TOKEN));
    }

    @Test
    @DisplayName("Decisión 4: si Identidad no responde se responde 503")
    void identidadCaida() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withException(new IOException("Sin conexión")));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarAnfitrion(TOKEN));
    }

    @Test
    @DisplayName("Decisión 4: si Identidad falla con un error interno se responde 503")
    void identidadConError() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarAnfitrion(TOKEN));
    }
    @Test
@DisplayName("CA-039: registra la aprobacion en el microservicio de Auditoria")
void registraAuditoriaDeAprobacion() {
    EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
            10L,
            "PUBLICACION",
            25L,
            "APROBAR_PUBLICACION",
            "EXITOSO",
            "El administrador aprobo la publicacion"
    );

    servidor.expect(requestTo("http://localhost:8090/internal/audit-events"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString("\"usuarioId\":10")))
            .andExpect(content().string(containsString("\"entidadId\":25")))
            .andExpect(content().string(containsString("\"accion\":\"APROBAR_PUBLICACION\"")))
            .andRespond(withStatus(HttpStatus.CREATED));

    assertTrue(serviciosExternos.registrarAuditoria(evento));

    servidor.verify();
}
@Test
@DisplayName("CA-039: notifica al anfitrion cuando aprueban su publicacion")
void notificaPublicacionAprobada() {
    Long anfitrionId = 7L;
    Long publicacionId = 25L;

    servidor.expect(requestTo("http://localhost:8088/internal/notifications"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                    {
                      "usuarioId": 7,
                      "tipoEvento": "PUBLICACION_APROBADA",
                      "entidadTipo": "PUBLICACION",
                      "entidadId": 25,
                      "mensaje": "Tu publicacion ha sido aprobada y ahora esta activa."
                    }
                    """))
            .andRespond(withStatus(HttpStatus.CREATED));

    assertTrue(serviciosExternos.notificarPublicacionAprobada(
            anfitrionId, publicacionId));

        servidor.verify();
}

@Test
@DisplayName("CA-039: notifica al anfitrion cuando rechazan su publicacion")
void notificaPublicacionRechazada() {
    Long anfitrionId = 7L;
    Long publicacionId = 25L;
    String motivo = "La publicacion no cumple con los requisitos";

    servidor.expect(requestTo("http://localhost:8088/internal/notifications"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                    {
                      "usuarioId": 7,
                      "tipoEvento": "PUBLICACION_RECHAZADA",
                      "entidadTipo": "PUBLICACION",
                      "entidadId": 25,
                      "mensaje": "Tu publicacion fue rechazada. Motivo: La publicacion no cumple con los requisitos"
                    }
                    """))
            .andRespond(withStatus(HttpStatus.CREATED));

    assertTrue(serviciosExternos.notificarPublicacionRechazada(
            anfitrionId, publicacionId, motivo));

        servidor.verify();
}

@Test
@DisplayName("CA-039: maneja el error cuando Auditoria no esta disponible")
void auditoriaNoDisponible() {
    EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
            10L,
            "PUBLICACION",
            25L,
            "APROBAR_PUBLICACION",
            "EXITOSO",
            "El administrador aprobo la publicacion"
    );

    servidor.expect(requestTo("http://localhost:8090/internal/audit-events"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    assertFalse(serviciosExternos.registrarAuditoria(evento));

    servidor.verify();
}
@Test
@DisplayName("CA-039: maneja el error cuando Notificaciones no esta disponible")
void notificacionesNoDisponible() {
    servidor.expect(requestTo("http://localhost:8088/internal/notifications"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    assertFalse(serviciosExternos.notificarPublicacionAprobada(7L, 25L));

    servidor.verify();
}
}
