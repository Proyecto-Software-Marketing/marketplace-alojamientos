package co.edu.unbosque.marketplace.reportes.client;

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

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.reportes.dto.EstadisticasReservas;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasUsuarios;
import co.edu.unbosque.marketplace.reportes.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.reportes.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;

// Pruebas de las llamadas a Identidad, Alojamientos y Reservas simulado
class ServiciosExternosTest {

    private static final String URL_AUTORIZAR = "http://identidad/autenticacion/autorizar?roles=ADMINISTRADOR";
    private static final String TOKEN = "Bearer token-de-prueba";

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, "http://identidad", "http://alojamientos",
                "http://reservas", "http://auditoria");
    }

    @Test
    void elAdministradorPasa() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> serviciosExternos.verificarAdministrador(TOKEN));
        servidor.verify();
    }

    @Test
    void sinSesionValidaDa401() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAdministrador(TOKEN));
    }

    @Test
    void siNoEsAdministradorDa403() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class, () -> serviciosExternos.verificarAdministrador(TOKEN));
    }

    @Test
    void sinTokenNiSiquieraPreguntaAIdentidad() {
        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAdministrador(null));
        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarAdministrador(" "));
    }

    @Test
    void traeLasEstadisticasDeUsuarios() {
        servidor.expect(requestTo("http://identidad/interno/usuarios/estadisticas"))
                .andRespond(withSuccess("{\"total\":10,\"huespedes\":7,\"anfitriones\":2}", MediaType.APPLICATION_JSON));

        EstadisticasUsuarios usuarios = serviciosExternos.estadisticasUsuarios();

        assertEquals(10, usuarios.getTotal());
        assertEquals(7, usuarios.getHuespedes());
        assertEquals(2, usuarios.getAnfitriones());
    }

    @Test
    void siIdentidadNoMandaNadaQuedaEnCero() {
        servidor.expect(requestTo("http://identidad/interno/usuarios/estadisticas")).andRespond(withSuccess());

        assertEquals(0, serviciosExternos.estadisticasUsuarios().getTotal());
    }

    @Test
    void traeLasPublicacionesActivas() {
        servidor.expect(requestTo("http://alojamientos/interno/publicaciones/estadisticas"))
                .andRespond(withSuccess("{\"activas\":4}", MediaType.APPLICATION_JSON));

        assertEquals(4, serviciosExternos.estadisticasPublicaciones().getActivas());
    }

    @Test
    void traeLasEstadisticasDeReservas() {
        servidor.expect(requestTo("http://reservas/interno/reservas/estadisticas"))
                .andRespond(withSuccess("{\"porEstado\":{\"CONFIRMADA\":6},\"ingresosEstimados\":2500000,"
                        + "\"ciudadesMayorDemanda\":[{\"ciudad\":\"Bogota\",\"reservas\":8}]}", MediaType.APPLICATION_JSON));

        EstadisticasReservas reservas = serviciosExternos.estadisticasReservas();

        assertEquals(6L, reservas.getPorEstado().get("CONFIRMADA"));
        assertEquals(0, new BigDecimal("2500000").compareTo(reservas.getIngresosEstimados()));
        assertEquals("Bogota", reservas.getCiudadesMayorDemanda().get(0).getCiudad());
    }

    @Test
    void sinAlojamientosNiReservasConfiguradosQuedaEnCero() {
        ServiciosExternos sinOtros = new ServiciosExternos(RestClient.builder(), "http://identidad", "", "",
                "http://auditoria");

        assertEquals(0, sinOtros.estadisticasPublicaciones().getActivas());
        assertEquals(0, BigDecimal.ZERO.compareTo(sinOtros.estadisticasReservas().getIngresosEstimados()));
    }

    @Test
    @DisplayName("CA-113: el administrador que exporta queda identificado con su id")
    void elAdministradorQueExportaQuedaIdentificado() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));

        assertEquals(7L, serviciosExternos.verificarAdministradorParaExportar(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("CA-113: si no es administrador no puede exportar")
    void sinPermisoNoExporta() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        PermisoException error = assertThrows(PermisoException.class,
                () -> serviciosExternos.verificarAdministradorParaExportar(TOKEN));

        assertEquals("Solo el administrador puede exportar los reportes", error.getMessage());
    }

    @Test
    @DisplayName("CA-113: la exportacion queda registrada en Auditoria")
    void registraLaExportacionEnAuditoria() {
        servidor.expect(requestTo("http://auditoria/internal/audit-events"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        boolean registrado = serviciosExternos.registrarAuditoria(
                new EventoAuditoriaDTO(7L, "REPORTE", null, "EXPORTAR_REPORTE", "EXITO",
                        "Reporte: Usuarios; Formato: PDF; Archivo: reporte-usuarios-2026-11-16.pdf"));

        assertTrue(registrado);
        servidor.verify();
    }

    @Test
    @DisplayName("CA-113: si Auditoria no responde la exportacion igual se entrega")
    void auditoriaCaidaNoRompeLaExportacion() {
        servidor.expect(requestTo("http://auditoria/internal/audit-events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        boolean registrado = serviciosExternos.registrarAuditoria(
                new EventoAuditoriaDTO(7L, "REPORTE", null, "EXPORTAR_REPORTE", "EXITO", "Reporte: Usuarios"));

        assertFalse(registrado);
        servidor.verify();
    }
}