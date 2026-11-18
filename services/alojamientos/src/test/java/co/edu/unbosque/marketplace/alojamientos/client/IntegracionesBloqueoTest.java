package co.edu.unbosque.marketplace.alojamientos.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IntegracionesBloqueoTest {

    private MockRestServiceServer servidor;
    private IntegracionesBloqueo integraciones;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();

        integraciones = new IntegracionesBloqueo(
                constructor,
                "http://localhost:8086",
                "http://localhost:8088",
                "http://localhost:8090");
    }

    @Test
    void enviaNotificacionAlAnfitrion() {
        servidor.expect(requestTo("http://localhost:8088/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "usuarioId": 7,
                          "tipoEvento": "PUBLICACION_BLOQUEADA",
                          "entidadTipo": "PUBLICACION",
                          "entidadId": 15,
                          "mensaje": "Tu publicacion fue bloqueada por un administrador. Motivo: Incumple las reglas"
                        }
                        """))
                .andRespond(withSuccess());

        assertTrue(integraciones.notificarBloqueo(
                7L, 15L, "Incumple las reglas"));

        servidor.verify();
    }

    @Test
    void registraBloqueoEnAuditoria() {
        servidor.expect(requestTo("http://localhost:8090/internal/audit-events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "usuarioId": 1,
                          "entidad": "PUBLICACION",
                          "entidadId": 15,
                          "accion": "BLOQUEAR_PUBLICACION",
                          "resultado": "EXITO",
                          "detalle": "Incumple las reglas"
                        }
                        """))
                .andRespond(withSuccess());

        assertTrue(integraciones.registrarAuditoria(
                1L, 15L, "Incumple las reglas"));

        servidor.verify();
    }

    @Test
    void notificacionFallidaDevuelveFalse() {
        servidor.expect(requestTo("http://localhost:8088/internal/notifications"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertFalse(integraciones.notificarBloqueo(
                7L, 15L, "Incumple las reglas"));

        servidor.verify();
    }

    @Test
    void auditoriaFallidaDevuelveFalse() {
        servidor.expect(requestTo("http://localhost:8090/internal/audit-events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertFalse(integraciones.registrarAuditoria(
                1L, 15L, "Incumple las reglas"));

        servidor.verify();
    }

    @Test
    void mensajeNotificacionNoSupera500Caracteres() {
        String motivoLargo = "A".repeat(2000);

        servidor.expect(requestTo("http://localhost:8088/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String cuerpo = ((org.springframework.mock.http.client.MockClientHttpRequest)
                            request).getBodyAsString();

                    String prefijo = "\"mensaje\":\"";
                    int inicio = cuerpo.indexOf(prefijo);

                    assertTrue(inicio >= 0, "Debe incluir el campo mensaje");

                    inicio += prefijo.length();
                    int fin = cuerpo.indexOf("\"", inicio);

                    assertTrue(fin > inicio, "El mensaje debe tener contenido");
                    assertTrue(fin - inicio <= 500,
                            "El mensaje no debe superar 500 caracteres");
                })
                .andRespond(withSuccess());

        assertTrue(integraciones.notificarBloqueo(
                7L, 15L, motivoLargo));

        servidor.verify();
    }

    @Test
    void solicitaRevisionManualDeReservas() {
        servidor.expect(requestTo("http://localhost:8086/internal/bookings/review-manual"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "publicacionId": 15
                        }
                        """))
                .andRespond(withSuccess());

        assertTrue(integraciones.solicitarRevisionManual(15L));

        servidor.verify();
    }

    @Test
    void errorEnReservasDevuelveFalse() {
        servidor.expect(requestTo("http://localhost:8086/internal/bookings/review-manual"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertFalse(integraciones.solicitarRevisionManual(15L));

        servidor.verify();
    }
}