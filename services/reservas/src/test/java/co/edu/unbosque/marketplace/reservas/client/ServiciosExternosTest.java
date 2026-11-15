package co.edu.unbosque.marketplace.reservas.client;

import co.edu.unbosque.marketplace.reservas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.ServicioNoDisponibleException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
        serviciosExternos = new ServiciosExternos(constructor, URL_IDENTIDAD, URL_DISPONIBILIDAD,
                URL_COTIZACION, URL_NOTIFICACIONES, URL_ALOJAMIENTOS);
    }

    @Test
    @DisplayName("verificarRolHuesped devuelve el id si el usuario es HUESPED")
    void verificarRolHuespedExitoso() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":30,\"rol\":\"HUESPED\"}", MediaType.APPLICATION_JSON));

        assertEquals(30L, serviciosExternos.verificarRolHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarRolHuesped lanza PermisoException si no es HUESPED")
    void verificarRolHuespedSinPermiso() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class, () -> serviciosExternos.verificarRolHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarRolHuesped lanza AutenticacionException si el token no sirve")
    void verificarRolHuespedTokenInvalido() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarRolHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarRolHuesped lanza ServicioNoDisponibleException si Identidad no responde")
    void verificarRolHuespedServicioCaido() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarRolHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("consultarCotizacion devuelve los datos de la cotización")
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

        CotizacionDTO cotizacion = serviciosExternos.consultarCotizacion("cot-123");

        assertEquals("cot-123", cotizacion.id());
        assertEquals(15L, cotizacion.publicacionId());
        assertEquals(2, cotizacion.cantidadHuespedes());
        servidor.verify();
    }

    @Test
    @DisplayName("consultarCotizacion lanza NoEncontradoException si no existe")
    void consultarCotizacionNoExiste() {
        servidor.expect(requestTo(URL_COTIZACION + "/quotes/cot-inexistente"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> serviciosExternos.consultarCotizacion("cot-inexistente"));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarDisponibilidad responde true cuando la publicación está disponible")
    void verificarDisponibilidadTrue() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"disponibles\":[15,20]}", MediaType.APPLICATION_JSON));

        assertTrue(serviciosExternos.verificarDisponibilidad(15L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
        servidor.verify();
    }

    @Test
    @DisplayName("verificarDisponibilidad responde false cuando no está disponible")
    void verificarDisponibilidadFalse() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/check"))
                .andRespond(withSuccess("{\"disponibles\":[20]}", MediaType.APPLICATION_JSON));

        assertFalse(serviciosExternos.verificarDisponibilidad(15L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
        servidor.verify();
    }

    @Test
    @DisplayName("bloquearPeriodo hace POST /internal/availability/block")
    void bloquearPeriodoExitoso() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/block"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> serviciosExternos.bloquearPeriodo(15L, 100L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-065 bloquearPeriodo lanza ConflictoReservaException si hay 409")
    void bloquearPeriodoConflicto() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/block"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(ConflictoReservaException.class, () -> serviciosExternos.bloquearPeriodo(15L, 100L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
        servidor.verify();
    }

    @Test
    @DisplayName("notificarReservaConfirmada hace POST /internal/notifications")
    void notificarReservaConfirmadaExitoso() {
        servidor.expect(requestTo(URL_NOTIFICACIONES + "/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> serviciosExternos.notificarReservaConfirmada(30L, 100L, "RES-TEST", "Confirmada"));
        servidor.verify();
    }

    @Test
    @DisplayName("Si Notificaciones falla no se rompe la reserva (best-effort)")
    void notificarReservaConfirmadaNoRompeElFlujo() {
        servidor.expect(requestTo(URL_NOTIFICACIONES + "/internal/notifications"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertDoesNotThrow(() -> serviciosExternos.notificarReservaConfirmada(30L, 100L, "RES-TEST", "Confirmada"));
        servidor.verify();
    }

    @Test
    @DisplayName("consultarAnfitrion devuelve el id del dueño de la publicación")
    void consultarAnfitrionExitoso() {
        servidor.expect(requestTo(URL_ALOJAMIENTOS + "/internal/listings/15/owner"))
                .andRespond(withSuccess("{\"anfitrionId\":88}", MediaType.APPLICATION_JSON));

        assertEquals(88L, serviciosExternos.consultarAnfitrion(15L));
        servidor.verify();
    }

    @Test
    @DisplayName("consultarAnfitrion devuelve null si Alojamientos no está disponible")
    void consultarAnfitrionNoDisponible() {
        servidor.expect(requestTo(URL_ALOJAMIENTOS + "/internal/listings/15/owner"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertNull(serviciosExternos.consultarAnfitrion(15L));
        servidor.verify();
    }
}
