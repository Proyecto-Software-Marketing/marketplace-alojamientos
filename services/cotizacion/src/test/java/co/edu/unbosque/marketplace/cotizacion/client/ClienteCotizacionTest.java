package co.edu.unbosque.marketplace.cotizacion.client;

import co.edu.unbosque.marketplace.cotizacion.dto.DatosPublicacion;
import co.edu.unbosque.marketplace.cotizacion.exception.AutenticacionException;
import co.edu.unbosque.marketplace.cotizacion.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.cotizacion.exception.PermisoException;
import co.edu.unbosque.marketplace.cotizacion.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Pruebas de la HU-22 con Identidad y Alojamientos simulados
class ClienteCotizacionTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_ALOJAMIENTOS = "http://alojamientos";
    private static final String URL_AUTORIZAR = URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED";
    private static final String URL_DATOS = URL_ALOJAMIENTOS + "/internal/listings/1/quote-data";
    private static final String TOKEN = "Bearer token-huesped";

    private MockRestServiceServer servidor;
    private ClienteCotizacion clienteCotizacion;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        clienteCotizacion = new ClienteCotizacion(constructor, URL_IDENTIDAD, URL_ALOJAMIENTOS);
    }

    // ---------- Identidad ----------

    @Test
    @DisplayName("Decisión 1: el huésped con sesión obtiene su id desde el token")
    void huespedConSesion() {
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":2,\"rol\":\"HUESPED\"}", MediaType.APPLICATION_JSON));

        assertEquals(2L, clienteCotizacion.verificarHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("Decisión 1: un anfitrión o administrador recibe 403 de Identidad")
    void noEsHuesped() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        PermisoException error = assertThrows(PermisoException.class,
                () -> clienteCotizacion.verificarHuesped(TOKEN));
        assertEquals("Solo un huésped puede pedir una cotización", error.getMessage());
    }

    @Test
    @DisplayName("Decisión 1: con el token vencido o sin id de usuario se responde 401")
    void sinSesion() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThrows(AutenticacionException.class, () -> clienteCotizacion.verificarHuesped(TOKEN));

        servidor.reset();
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(AutenticacionException.class, () -> clienteCotizacion.verificarHuesped(TOKEN));
    }

    @Test
    @DisplayName("Decisión 1: si Identidad no responde se responde 503")
    void identidadCaida() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withException(new IOException("Sin conexión")));

        assertThrows(ServicioNoDisponibleException.class, () -> clienteCotizacion.verificarHuesped(TOKEN));
    }

    // ---------- Alojamientos ----------

    @Test
    @DisplayName("Decisión 5: lee el quote-data de Alojamientos (HU-10), ignorando campos que no usa")
    void datosPublicacion() {
        servidor.expect(requestTo(URL_DATOS))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"publicacionId\":1,\"estado\":\"ACTIVA\",\"capacidad\":4,"
                        + "\"precioPorNoche\":250000,\"tarifaLimpieza\":60000,"
                        + "\"porcentajeDescuentoEstanciaLarga\":10,\"nochesMinimasDescuento\":7,"
                        + "\"serviciosAdicionales\":[{\"nombre\":\"Desayuno\",\"costo\":25000}],"
                        + "\"campoNuevo\":\"x\"}", MediaType.APPLICATION_JSON));

        DatosPublicacion datos = clienteCotizacion.consultarDatosPublicacion(1L);

        assertEquals(1L, datos.getPublicacionId());
        assertEquals("ACTIVA", datos.getEstado());
        assertEquals(4, datos.getCapacidad());
        assertEquals(0, new BigDecimal("250000").compareTo(datos.getPrecioPorNoche()));
        assertEquals(0, new BigDecimal("60000").compareTo(datos.getTarifaLimpieza()));
        assertEquals(10, datos.getPorcentajeDescuentoEstanciaLarga());
        assertEquals(7, datos.getNochesMinimasDescuento());
        assertEquals("Desayuno", datos.getServiciosAdicionales().get(0).getNombre());
        assertEquals(0, new BigDecimal("25000").compareTo(datos.getServiciosAdicionales().get(0).getCosto()));
    }

    @Test
    @DisplayName("Decisión 5: sin descuento llegan las noches mínimas en null y sin servicios adicionales")
    void datosSinDescuento() {
        servidor.expect(requestTo(URL_DATOS)).andRespond(withSuccess("{\"publicacionId\":1,\"estado\":\"ACTIVA\","
                + "\"capacidad\":2,\"precioPorNoche\":90000,\"tarifaLimpieza\":0,"
                + "\"porcentajeDescuentoEstanciaLarga\":0,\"nochesMinimasDescuento\":null,"
                + "\"serviciosAdicionales\":null}", MediaType.APPLICATION_JSON));

        DatosPublicacion datos = clienteCotizacion.consultarDatosPublicacion(1L);

        assertNull(datos.getNochesMinimasDescuento());
        assertTrue(datos.getServiciosAdicionales().isEmpty());
    }

    @Test
    @DisplayName("Decisión 5: si la publicación no existe se responde 404")
    void publicacionNoExiste() {
        servidor.expect(requestTo(URL_DATOS)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        NoEncontradoException error = assertThrows(NoEncontradoException.class,
                () -> clienteCotizacion.consultarDatosPublicacion(1L));
        assertEquals("La publicación no existe", error.getMessage());
    }

    @Test
    @DisplayName("Decisión 5: si Alojamientos no responde, falla o responde vacío se responde 503")
    void alojamientosCaido() {
        servidor.expect(requestTo(URL_DATOS)).andRespond(withException(new IOException("Sin conexión")));
        assertThrows(ServicioNoDisponibleException.class, () -> clienteCotizacion.consultarDatosPublicacion(1L));

        servidor.reset();
        servidor.expect(requestTo(URL_DATOS)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(ServicioNoDisponibleException.class, () -> clienteCotizacion.consultarDatosPublicacion(1L));

        servidor.reset();
        servidor.expect(requestTo(URL_DATOS)).andRespond(withSuccess());
        assertThrows(ServicioNoDisponibleException.class, () -> clienteCotizacion.consultarDatosPublicacion(1L));
    }
}
