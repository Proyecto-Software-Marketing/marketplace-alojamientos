
package co.edu.unbosque.marketplace.alojamientos.client;

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
}
