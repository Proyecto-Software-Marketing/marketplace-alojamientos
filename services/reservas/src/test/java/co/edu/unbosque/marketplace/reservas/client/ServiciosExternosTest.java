package co.edu.unbosque.marketplace.reservas.client;

import co.edu.unbosque.marketplace.reservas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServiciosExternosTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_DISPONIBILIDAD = "http://disponibilidad";
    private static final String URL_COTIZACION = "http://cotizacion";
    private static final String URL_NOTIFICACIONES = "http://notificaciones";
    private static final String URL_ALOJAMIENTOS = "http://alojamientos";
    private static final String TOKEN = "Bearer token-prueba";

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(
                constructor,
                URL_IDENTIDAD,
                URL_DISPONIBILIDAD,
                URL_COTIZACION,
                URL_NOTIFICACIONES,
                URL_ALOJAMIENTOS
        );
    }

    @Test
    @DisplayName("verificarRolHuesped retorna el ID del usuario si tiene rol HUESPED")
    void verificarRolHuespedExitoso() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":30,\"rol\":\"HUESPED\"}", MediaType.APPLICATION_JSON));

        Long id = serviciosExternos.verificarRolHuesped(TOKEN);
        assertEquals(30L, id);
        servidor.verify();
    }

    @Test
    @DisplayName("verificarRolHuesped lanza PermisoException (403) si no es HUÉSPED")
    void verificarRolHuespedNoAutorizado() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class, () -> serviciosExternos.verificarRolHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarRolHuesped lanza AutenticacionException (401) si token es inválido")
    void verificarRolHuespedTokenInvalido() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarRolHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarDisponibilidad responde true cuando la publicación está en la lista de disponibles")
    void verificarDisponibilidadTrue() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"disponibles\":[15,20]}", MediaType.APPLICATION_JSON));

        boolean disponible = serviciosExternos.verificarDisponibilidad(15L, LocalDate.now(), LocalDate.now().plusDays(2));
        assertTrue(disponible);
        servidor.verify();
    }

    @Test
    @DisplayName("verificarDisponibilidad responde false cuando la publicación no está disponible")
    void verificarDisponibilidadFalse() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"disponibles\":[20]}", MediaType.APPLICATION_JSON));

        boolean disponible = serviciosExternos.verificarDisponibilidad(15L, LocalDate.now(), LocalDate.now().plusDays(2));
        assertFalse(disponible);
        servidor.verify();
    }

    @Test
    @DisplayName("bloquearPeriodo ejecuta POST /internal/availability/block exitosamente")
    void bloquearPeriodoExitoso() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/block"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> serviciosExternos.bloquearPeriodo(15L, 100L, LocalDate.now(), LocalDate.now().plusDays(2)));
        servidor.verify();
    }

    @Test
    @DisplayName("bloquearPeriodo lanza ConflictoReservaException si disponibilidad devuelve 409")
    void bloquearPeriodoConflicto() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/block"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(ConflictoReservaException.class, () ->
                serviciosExternos.bloquearPeriodo(15L, 100L, LocalDate.now(), LocalDate.now().plusDays(2)));
        servidor.verify();
    }

    @Test
    @DisplayName("notificarReservaConfirmada envía POST /internal/notifications")
    void notificarReservaConfirmadaExitoso() {
        servidor.expect(requestTo(URL_NOTIFICACIONES + "/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> serviciosExternos.notificarReservaConfirmada(30L, 100L, "RES-TEST"));
        servidor.verify();
    }

    @Test
    @DisplayName("consultarCotizacion retorna CotizacionDTO cuando existe")
    void consultarCotizacionExitosa() {
        String json = """
                {
                    "id": "cot-123",
                    "publicacionId": 15,
                    "fechaLlegada": "2026-12-01",
                    "fechaSalida": "2026-12-05",
                    "cantidadHuespedes": 2,
                    "total": 500000.00
                }
                """;
        servidor.expect(requestTo(URL_COTIZACION + "/quotes/cot-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        CotizacionDTO cot = serviciosExternos.consultarCotizacion("cot-123");
        assertEquals("cot-123", cot.id());
        assertEquals(15L, cot.publicacionId());
        servidor.verify();
    }

    @Test
    @DisplayName("consultarCotizacion lanza NoEncontradoException cuando no existe (404)")
    void consultarCotizacionNoExiste() {
        servidor.expect(requestTo(URL_COTIZACION + "/quotes/cot-inexistente"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> serviciosExternos.consultarCotizacion("cot-inexistente"));
        servidor.verify();
    }
}
