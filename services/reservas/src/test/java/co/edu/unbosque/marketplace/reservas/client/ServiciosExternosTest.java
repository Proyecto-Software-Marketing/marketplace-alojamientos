package co.edu.unbosque.marketplace.reservas.client;

import co.edu.unbosque.marketplace.reservas.dto.EventoAuditoriaDTO;
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

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServiciosExternosTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_DISPONIBILIDAD = "http://disponibilidad";
    private static final String URL_COTIZACION = "http://cotizacion";
    private static final String URL_NOTIFICACIONES = "http://notificaciones";
    private static final String URL_ALOJAMIENTOS = "http://alojamientos";
    private static final String URL_AUDITORIA = "http://auditoria";
    private static final String URL_EVENTOS = URL_AUDITORIA + "/internal/audit-events";
    private static final String TOKEN = "Bearer token-prueba";

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, URL_IDENTIDAD, URL_DISPONIBILIDAD,
                URL_COTIZACION, URL_NOTIFICACIONES, URL_ALOJAMIENTOS, URL_AUDITORIA);
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
    @DisplayName("HU-26 verificarSesion devuelve id y rol para HUESPED o ANFITRION")
    void verificarSesionExitosa() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED,ANFITRION"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":88,\"rol\":\"ANFITRION\"}", MediaType.APPLICATION_JSON));

        UsuarioIdentidadDTO usuario = serviciosExternos.verificarSesion(TOKEN);

        assertEquals(88L, usuario.id());
        assertEquals("ANFITRION", usuario.rol());
        servidor.verify();
    }

    @Test
    @DisplayName("HU-26 verificarSesion lanza PermisoException si el rol no está permitido")
    void verificarSesionSinPermiso() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED,ANFITRION"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class, () -> serviciosExternos.verificarSesion(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("HU-26 verificarSesion lanza AutenticacionException sin token")
    void verificarSesionSinToken() {
        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarSesion(" "));
    }

    @Test
    @DisplayName("HU-28 verificarSesionCancelacion permite HUESPED y ADMINISTRADOR")
    void verificarSesionCancelacionExitosa() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED,ADMINISTRADOR"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":1,\"rol\":\"ADMINISTRADOR\"}", MediaType.APPLICATION_JSON));

        UsuarioIdentidadDTO usuario = serviciosExternos.verificarSesionCancelacion(TOKEN);

        assertEquals(1L, usuario.id());
        assertEquals("ADMINISTRADOR", usuario.rol());
        servidor.verify();
    }

    @Test
    @DisplayName("CA-079 verificarSesionCancelacion lanza PermisoException para el anfitrión")
    void verificarSesionCancelacionSinPermiso() {
        servidor.expect(requestTo(URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED,ADMINISTRADOR"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarSesionCancelacion(TOKEN));
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
    @DisplayName("CA-080 liberarPeriodo hace POST /internal/availability/release")
    void liberarPeriodoExitoso() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.publicacionId").value(15))
                .andExpect(jsonPath("$.reservaId").value(100))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> serviciosExternos.liberarPeriodo(15L, 100L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-080 si Disponibilidad falla al liberar, se informa servicio no disponible")
    void liberarPeriodoNoDisponible() {
        servidor.expect(requestTo(URL_DISPONIBILIDAD + "/internal/availability/release"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.liberarPeriodo(15L, 100L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-077 consultarPoliticaCancelacion devuelve la política de la publicación")
    void consultarPoliticaCancelacionExitosa() {
        String json = """
                {
                    "id": "pol-1",
                    "nombre": "Flexible",
                    "descripcion": "Devolución del 50%",
                    "porcentajeDevolucion": 50,
                    "permiteLiberacionPeriodo": true
                }
                """;
        servidor.expect(requestTo(URL_ALOJAMIENTOS + "/internal/listings/15/cancellation-policy"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        PoliticaCancelacionDTO politica = serviciosExternos.consultarPoliticaCancelacion(15L);

        assertEquals("Flexible", politica.nombre());
        assertEquals(0, politica.porcentajeDevolucion().compareTo(new java.math.BigDecimal("50")));
        assertTrue(politica.permiteLiberacionPeriodo());
        servidor.verify();
    }

    @Test
    @DisplayName("CA-077 si Alojamientos no encuentra la publicación, responde no encontrado")
    void consultarPoliticaCancelacionNoExiste() {
        servidor.expect(requestTo(URL_ALOJAMIENTOS + "/internal/listings/15/cancellation-policy"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class,
                () -> serviciosExternos.consultarPoliticaCancelacion(15L));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-077 si Alojamientos falla, responde servicio no disponible")
    void consultarPoliticaCancelacionNoDisponible() {
        servidor.expect(requestTo(URL_ALOJAMIENTOS + "/internal/listings/15/cancellation-policy"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.consultarPoliticaCancelacion(15L));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-077 una política vacía se considera inválida")
    void consultarPoliticaCancelacionVacia() {
        servidor.expect(requestTo(URL_ALOJAMIENTOS + "/internal/listings/15/cancellation-policy"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.consultarPoliticaCancelacion(15L));
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
    @DisplayName("CA-128 notificarReservaCancelada hace POST /internal/notifications")
    void notificarReservaCanceladaExitoso() {
        servidor.expect(requestTo(URL_NOTIFICACIONES + "/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.tipoEvento").value("RESERVA_CANCELADA"))
                .andExpect(jsonPath("$.entidadTipo").value("RESERVA"))
                .andExpect(jsonPath("$.entidadId").value(100))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> serviciosExternos.notificarReservaCancelada(30L, 100L, "RES-TEST", "Cancelada"));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-128 si Notificaciones falla, la cancelación no se rompe (best-effort)")
    void notificarReservaCanceladaNoRompeElFlujo() {
        servidor.expect(requestTo(URL_NOTIFICACIONES + "/internal/notifications"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertDoesNotThrow(() -> serviciosExternos.notificarReservaCancelada(30L, 100L, "RES-TEST", "Cancelada"));
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

    @Test
    @DisplayName("CA-081 registrarAuditoria envía la cancelación a Auditoría")
    void registrarAuditoriaExitoso() {
        String detalle = "Fecha de cancelación: 2026-01-01T10:00; Motivo: Viaje cancelado; "
                + "Devolución simulada: 225000.00; Estado final: CANCELADA";
        servidor.expect(requestTo(URL_EVENTOS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.usuarioId").value(25))
                .andExpect(jsonPath("$.entidad").value("RESERVA"))
                .andExpect(jsonPath("$.entidadId").value(1))
                .andExpect(jsonPath("$.accion").value("CANCELAR_RESERVA"))
                .andExpect(jsonPath("$.resultado").value("EXITO"))
                .andExpect(jsonPath("$.detalle").value(detalle))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertTrue(serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(25L, "RESERVA", 1L,
                "CANCELAR_RESERVA", "EXITO", detalle)));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-082 si Auditoría no responde no se lanza error, solo se informa")
    void auditoriaNoDisponible() {
        servidor.expect(requestTo(URL_EVENTOS)).andRespond(withException(new IOException("Sin conexión")));

        assertFalse(serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(25L, "RESERVA", 1L,
                "CANCELAR_RESERVA", "EXITO", "Detalle")));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-082 si Auditoría responde con error no se lanza error, solo se informa")
    void auditoriaConError() {
        servidor.expect(requestTo(URL_EVENTOS)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertFalse(serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(25L, "RESERVA", 1L,
                "CANCELAR_RESERVA", "EXITO", "Detalle")));
        servidor.verify();
    }
}
