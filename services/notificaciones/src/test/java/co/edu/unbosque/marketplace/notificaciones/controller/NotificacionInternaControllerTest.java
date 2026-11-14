package co.edu.unbosque.marketplace.notificaciones.controller;

import co.edu.unbosque.marketplace.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la ruta interna de la HU-30, como la llamarían Alojamientos y Reservas
@SpringBootTest
class NotificacionInternaControllerTest {

    private static final String RUTA = "/internal/notifications";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private Clock reloj;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        notificacionRepository.deleteAll();
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-14T23:05:00Z"));
        when(reloj.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    private ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String evento(long usuarioId, String tipoEvento, long entidadId) {
        return """
                {"usuarioId": %d, "tipoEvento": "%s", "entidadId": %d}
                """.formatted(usuarioId, tipoEvento, entidadId);
    }

    // ---------- CA-083 ----------

    @Test
    @DisplayName("CA-083 Alojamientos avisa una publicación aprobada y responde 201")
    void publicacionAprobadaResponde201() throws Exception {
        enviar(evento(7, "PUBLICACION_APROBADA", 15))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(jsonPath("$.tipoEvento").value("PUBLICACION_APROBADA"));

        assertEquals(1, notificacionRepository.count());
    }

    @Test
    @DisplayName("CA-083 Reservas avisa una reserva confirmada al huésped y al anfitrión: dos notificaciones")
    void reservaConfirmadaGeneraDosNotificaciones() throws Exception {
        enviar(evento(21, "RESERVA_CONFIRMADA", 300)).andExpect(status().isCreated());
        enviar(evento(7, "RESERVA_CONFIRMADA", 300)).andExpect(status().isCreated());

        assertEquals(2, notificacionRepository.count());
    }

    // ---------- CA-084 ----------

    @Test
    @DisplayName("CA-084 la respuesta incluye tipo de evento, fecha y entidad relacionada")
    void respuestaIncluyeTipoFechaYEntidad() throws Exception {
        enviar("""
                {"usuarioId": 21, "tipoEvento": "RESERVA_CONFIRMADA", "entidadTipo": "RESERVA",
                 "entidadId": 300, "mensaje": "Tu reserva fue confirmada"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoEvento").value("RESERVA_CONFIRMADA"))
                .andExpect(jsonPath("$.creadaEn").value(startsWith("2026-11-14T18:05")))
                .andExpect(jsonPath("$.entidadTipo").value("RESERVA"))
                .andExpect(jsonPath("$.entidadId").value(300))
                .andExpect(jsonPath("$.mensaje").value("Tu reserva fue confirmada"))
                .andExpect(jsonPath("$.leida").value(false));
    }

    // ---------- Decisiones y validaciones ----------

    @Test
    @DisplayName("Decisión 3: un reintento responde 200 con la misma notificación")
    void reintentoResponde200() throws Exception {
        enviar(evento(21, "RESERVA_CONFIRMADA", 300)).andExpect(status().isCreated());

        enviar(evento(21, "RESERVA_CONFIRMADA", 300)).andExpect(status().isOk());

        assertEquals(1, notificacionRepository.count());
    }

    @Test
    @DisplayName("400 si falta el usuario, el tipo de evento o la entidad")
    void faltanCamposResponde400() throws Exception {
        enviar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes").value(hasItem("El usuario es obligatorio")))
                .andExpect(jsonPath("$.mensajes").value(hasItem("El tipo de evento es obligatorio")))
                .andExpect(jsonPath("$.mensajes").value(hasItem("La entidad relacionada es obligatoria")));
    }

    @Test
    @DisplayName("Decisión 1: 400 si el tipo de evento no existe")
    void tipoDesconocidoResponde400() throws Exception {
        enviar(evento(21, "PAGO_RECIBIDO", 300))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value(startsWith("Tipo de evento no válido")));
    }

    @Test
    @DisplayName("Decisión 2: 400 si la entidad no corresponde al evento")
    void entidadIncorrectaResponde400() throws Exception {
        enviar("""
                {"usuarioId": 21, "tipoEvento": "RESERVA_CONFIRMADA", "entidadTipo": "PUBLICACION", "entidadId": 300}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 si el JSON está mal formado o un id no es número")
    void formatoIncorrectoResponde400() throws Exception {
        enviar("""
                {"usuarioId": "abc", "tipoEvento": "RESERVA_CONFIRMADA", "entidadId": 300}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }
}
