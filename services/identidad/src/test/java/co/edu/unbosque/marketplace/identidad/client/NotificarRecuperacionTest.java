package co.edu.unbosque.marketplace.identidad.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas del aviso a Notificaciones de la HU-04 con un servidor HTTP de prueba
class NotificarRecuperacionTest {

    private static final String MENSAJE = "Tu token para recuperar la contraseña es: abc. Vence en 30 minutos.";

    private HttpServer servidor;
    private ServiciosExternos serviciosExternos;
    private final AtomicInteger llamadas = new AtomicInteger();
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> tipoContenido = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private int estadoRespuesta = 201;

    // Servidor que hace de Notificaciones y guarda lo que recibe
    @BeforeEach
    void prepararServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/internal/notifications", intercambio -> {
            llamadas.incrementAndGet();
            metodo.set(intercambio.getRequestMethod());
            tipoContenido.set(intercambio.getRequestHeaders().getFirst("Content-Type"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            intercambio.sendResponseHeaders(estadoRespuesta, -1);
            intercambio.close();
        });
        servidor.start();

        serviciosExternos = new ServiciosExternos();
        ReflectionTestUtils.setField(serviciosExternos, "urlNotificaciones",
                "http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void apagarServidor() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("CA-010 envía a Notificaciones el evento con el usuario, la solicitud y el token")
    void enviaNotificacion() {
        serviciosExternos.notificarRecuperacion(21L, 5L, MENSAJE);

        assertEquals(1, llamadas.get());
        assertEquals("POST", metodo.get());
        assertTrue(tipoContenido.get().startsWith("application/json"));
        String json = cuerpo.get().replace(" ", "");
        assertTrue(json.contains("\"usuarioId\":21"));
        assertTrue(json.contains("\"tipoEvento\":\"RECUPERACION_CONTRASENA\""));
        assertTrue(json.contains("\"entidadTipo\":\"USUARIO\""));
        assertTrue(json.contains("\"entidadId\":5"));
        assertTrue(cuerpo.get().contains("abc. Vence en 30 minutos."));
    }

    @Test
    @DisplayName("Decisión 8: si la URL de Notificaciones está vacía no se llama")
    void sinUrlNoLlama() {
        ReflectionTestUtils.setField(serviciosExternos, "urlNotificaciones", "");

        assertDoesNotThrow(() -> serviciosExternos.notificarRecuperacion(21L, 5L, MENSAJE));
        assertEquals(0, llamadas.get());
    }

    @Test
    @DisplayName("Decisión 8: si Notificaciones responde con error se lanza la excepción del cliente")
    void notificacionesConError() {
        estadoRespuesta = 500;

        assertThrows(RestClientException.class, () -> serviciosExternos.notificarRecuperacion(21L, 5L, MENSAJE));
    }
}
