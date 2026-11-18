package co.edu.unbosque.marketplace.identidad.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ServiciosExternosHu35Test {

    private HttpServer servidor;
    private ServiciosExternos servicios;
    private final Map<String, String> cuerpos = new ConcurrentHashMap<>();
    private final Map<String, String> metodos = new ConcurrentHashMap<>();
    private final AtomicReference<String> respuestaReservas =
            new AtomicReference<>("true");

    private String baseUrl;

    @BeforeEach
    void preparar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        registrarRuta("/interno/reservas/futuras");
        registrarRuta("/interno/publicaciones/pausar");
        registrarRuta("/internal/notifications");
        registrarRuta("/internal/audit-events");

        servidor.start();

        baseUrl = "http://localhost:" + servidor.getAddress().getPort();

        servicios = new ServiciosExternos();

        ReflectionTestUtils.setField(servicios, "urlReservas", baseUrl);
        ReflectionTestUtils.setField(servicios, "urlAlojamientos", baseUrl);
        ReflectionTestUtils.setField(servicios, "urlNotificaciones", baseUrl);
        ReflectionTestUtils.setField(servicios, "urlAuditoria", baseUrl);
    }

    private void registrarRuta(String ruta) {
        servidor.createContext(ruta, intercambio -> {
            String path = intercambio.getRequestURI().getPath();

            metodos.put(path, intercambio.getRequestMethod());

            cuerpos.put(
                    path,
                    new String(
                            intercambio.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));

            byte[] respuesta = new byte[0];
            int estado = 204;

            if (path.equals("/interno/reservas/futuras")) {
                respuesta = respuestaReservas.get()
                        .getBytes(StandardCharsets.UTF_8);
                estado = 200;
            }

            intercambio.getResponseHeaders().set(
                    "Content-Type", "application/json");

            intercambio.sendResponseHeaders(
                    estado,
                    respuesta.length == 0 ? -1 : respuesta.length);

            if (respuesta.length > 0) {
                intercambio.getResponseBody().write(respuesta);
            }

            intercambio.close();
        });
    }

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void consultaReservasFuturas() {
        assertTrue(servicios.tieneReservasFuturas(25L));

        assertEquals(
                "GET",
                metodos.get("/interno/reservas/futuras"));

        assertTrue(
                cuerpos.get("/interno/reservas/futuras").isEmpty());

        respuestaReservas.set("false");

        assertFalse(servicios.tieneReservasFuturas(25L));
    }

    @Test
    void pausaPublicacionesDelAnfitrion() {
        servicios.pausarPublicaciones(30L);

        assertEquals(
                "POST",
                metodos.get("/interno/publicaciones/pausar"));
    }

    @Test
    void enviaNotificacionDeRestriccion() {
        servicios.notificarRestriccion(25L);

        String cuerpo = cuerpos.get("/internal/notifications");

        assertEquals(
                "POST",
                metodos.get("/internal/notifications"));

        assertTrue(cuerpo.contains("\"usuarioId\":25"));
        assertTrue(cuerpo.contains("USUARIO_RESTRINGIDO"));
        assertTrue(cuerpo.contains("\"entidadId\":25"));
        assertTrue(cuerpo.contains("restringida"));
    }

    @Test
    void enviaNotificacionDeReactivacion() {
        servicios.notificarReactivacion(25L);

        String cuerpo = cuerpos.get("/internal/notifications");

        assertEquals(
                "POST",
                metodos.get("/internal/notifications"));

        assertTrue(cuerpo.contains("\"usuarioId\":25"));
        assertTrue(cuerpo.contains("USUARIO_REACTIVADO"));
        assertTrue(cuerpo.contains("\"entidadId\":25"));
        assertTrue(cuerpo.contains("reactivada"));
    }

    @Test
    void registraAuditoriaDeUsuario() {
        servicios.registrarAuditoria(
                10L,
                25L,
                "USUARIO_RESTRINGIDO",
                "Cuenta restringida");

        String cuerpo = cuerpos.get("/internal/audit-events");

        assertEquals(
                "POST",
                metodos.get("/internal/audit-events"));

        assertTrue(cuerpo.contains("\"usuarioId\":10"));
        assertTrue(cuerpo.contains("\"entidad\":\"USUARIO\""));
        assertTrue(cuerpo.contains("\"entidadId\":25"));
        assertTrue(cuerpo.contains("USUARIO_RESTRINGIDO"));
        assertTrue(cuerpo.contains("EXITO"));
        assertTrue(cuerpo.contains("Cuenta restringida"));
    }

    @Test
    void noLlamaServiciosCuandoLasUrlEstanVacias() {
        ReflectionTestUtils.setField(servicios, "urlReservas", "");
        ReflectionTestUtils.setField(servicios, "urlAlojamientos", "");
        ReflectionTestUtils.setField(servicios, "urlNotificaciones", "");
        ReflectionTestUtils.setField(servicios, "urlAuditoria", "");

        assertFalse(servicios.tieneReservasFuturas(25L));

        assertDoesNotThrow(
                () -> servicios.pausarPublicaciones(30L));

        assertDoesNotThrow(
                () -> servicios.notificarRestriccion(25L));

        assertDoesNotThrow(
                () -> servicios.notificarReactivacion(25L));

        assertDoesNotThrow(
                () -> servicios.registrarAuditoria(
                        10L, 25L, "USUARIO_RESTRINGIDO", "Prueba"));

        assertTrue(metodos.isEmpty());
    }
    
    @Test
    void siLosServiciosFallanNoLanzaError() {
        // puerto donde no hay nadie escuchando
        ReflectionTestUtils.setField(servicios, "urlNotificaciones", "http://localhost:1");
        ReflectionTestUtils.setField(servicios, "urlAuditoria", "http://localhost:1");

        assertDoesNotThrow(() -> servicios.notificarRestriccion(25L));
        assertDoesNotThrow(() -> servicios.notificarReactivacion(25L));
        assertDoesNotThrow(() -> servicios.registrarAuditoria(10L, 25L, "USUARIO_RESTRINGIDO", "Prueba"));
    }
}