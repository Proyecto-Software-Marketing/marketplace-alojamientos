package co.edu.unbosque.marketplace.resenas.client;

import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.PermisoException;
import co.edu.unbosque.marketplace.resenas.exception.ServicioNoDisponibleException;
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

// Pruebas de la HU-32 con Identidad y Reservas simulados
class ServiciosExternosTest {

    private static final String URL_IDENTIDAD = "http://identidad";
    private static final String URL_RESERVAS = "http://reservas";
    private static final String URL_AUTORIZAR = URL_IDENTIDAD + "/autenticacion/autorizar?roles=HUESPED";
    private static final String URL_RESERVA = URL_RESERVAS + "/bookings/300";
    private static final String TOKEN = "Bearer token-de-prueba";
    private static final Long RESERVA = 300L;

    // Respuesta real de GET /bookings/{id} de Reservas (BookingResponseDTO)
    private static final String RESERVA_COMPLETADA = """
            {"id":300,"codigoReserva":"RES-ABC123","publicacionId":15,"huespedId":21,
             "fechaLlegada":"2026-11-01","fechaSalida":"2026-11-04","cantidadHuespedes":2,
             "montoTotal":450000.00,"estado":"COMPLETADA","creadaEn":"2026-10-20T09:30:00"}
            """;

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, URL_IDENTIDAD, URL_RESERVAS);
    }

    // ---------- Identidad: solo el huésped puede reseñar ----------

    @Test
    @DisplayName("HU-32 el huésped con sesión obtiene su id desde el token")
    void huespedConSesion() {
        // Identidad envía más campos de los que usa el DTO: se ignoran
        servidor.expect(requestTo(URL_AUTORIZAR))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("{\"id\":21,\"rol\":\"HUESPED\",\"nombre\":\"Ana\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(21L, serviciosExternos.verificarHuesped(TOKEN));
        servidor.verify();
    }

    @Test
    @DisplayName("HU-32 sin sesión se responde 401")
    void sinSesion() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarHuesped(TOKEN));
    }

    @Test
    @DisplayName("HU-32 un usuario que no es huésped recibe 403")
    void noEsHuesped() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(PermisoException.class, () -> serviciosExternos.verificarHuesped(TOKEN));
    }

    @Test
    @DisplayName("HU-32 si Identidad responde sin id de usuario se trata como sesión inválida")
    void identidadSinId() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.verificarHuesped(TOKEN));
    }

    @Test
    @DisplayName("Decisión 6: si Identidad no responde se responde 503")
    void identidadCaida() {
        servidor.expect(requestTo(URL_AUTORIZAR)).andRespond(withException(new IOException("Sin conexión")));

        assertThrows(ServicioNoDisponibleException.class, () -> serviciosExternos.verificarHuesped(TOKEN));
    }

    // ---------- Reservas: GET /bookings/{id} con el token del huésped ----------

    @Test
    @DisplayName("Decisión 2: se consulta la reserva en Reservas reenviando el token del huésped")
    void consultaReserva() {
        servidor.expect(requestTo(URL_RESERVA))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess(RESERVA_COMPLETADA, MediaType.APPLICATION_JSON));

        ReservaDTO reserva = serviciosExternos.consultarReserva(TOKEN, RESERVA);

        assertEquals(300L, reserva.id());
        assertEquals(15L, reserva.publicacionId());
        assertEquals(21L, reserva.huespedId());
        assertEquals("COMPLETADA", reserva.estado());
        servidor.verify();
    }

    @Test
    @DisplayName("Decisión 2: si la reserva no existe en Reservas se responde 404")
    void reservaNoExiste() {
        servidor.expect(requestTo(URL_RESERVA)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> serviciosExternos.consultarReserva(TOKEN, RESERVA));
    }

    @Test
    @DisplayName("Decisión 2: si Reservas responde 403 (reserva de otro usuario) se responde 404")
    void reservaDeOtroUsuario() {
        servidor.expect(requestTo(URL_RESERVA)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(NoEncontradoException.class, () -> serviciosExternos.consultarReserva(TOKEN, RESERVA));
    }

    @Test
    @DisplayName("Si Reservas rechaza el token se responde 401")
    void reservasRechazaToken() {
        servidor.expect(requestTo(URL_RESERVA)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutenticacionException.class, () -> serviciosExternos.consultarReserva(TOKEN, RESERVA));
    }

    @Test
    @DisplayName("Decisión 6: si Reservas falla se responde 503")
    void reservasConError() {
        servidor.expect(requestTo(URL_RESERVA)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.consultarReserva(TOKEN, RESERVA));
    }

    @Test
    @DisplayName("Decisión 6: si Reservas no responde se responde 503")
    void reservasCaido() {
        servidor.expect(requestTo(URL_RESERVA)).andRespond(withException(new IOException("Sin conexión")));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.consultarReserva(TOKEN, RESERVA));
    }

    @Test
    @DisplayName("Decisión 6: si Reservas responde sin estado no se puede validar la reseña (503)")
    void reservaSinEstado() {
        servidor.expect(requestTo(URL_RESERVA))
                .andRespond(withSuccess("{\"id\":300,\"publicacionId\":15,\"huespedId\":21}",
                        MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class,
                () -> serviciosExternos.consultarReserva(TOKEN, RESERVA));
    }
}
