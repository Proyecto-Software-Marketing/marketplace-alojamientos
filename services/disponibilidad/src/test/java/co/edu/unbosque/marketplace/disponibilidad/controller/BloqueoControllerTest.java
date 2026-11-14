package co.edu.unbosque.marketplace.disponibilidad.controller;

import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de las rutas internas de la HU-18, como las llamaría el servicio de Reservas
@SpringBootTest
class BloqueoControllerTest {

    private static final String RUTA = "/internal/availability";
    private static final Long PUBLICACION = 15L;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PeriodoDisponibleRepository periodoRepository;

    @Autowired
    private BloqueoReservaRepository bloqueoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        bloqueoRepository.deleteAll();
        periodoRepository.deleteAll();
        periodoRepository.save(new PeriodoDisponible(PUBLICACION, dia(1), dia(20)));
    }

    private LocalDate dia(int diasDesdeHoy) {
        return LocalDate.now().plusDays(diasDesdeHoy);
    }

    private String bloqueo(long idReserva, LocalDate llegada, LocalDate salida) {
        return """
                {"publicacionId": %d, "reservaId": %d, "fechaLlegada": "%s", "fechaSalida": "%s"}
                """.formatted(PUBLICACION, idReserva, llegada, salida);
    }

    private ResultActions enviar(String accion, String cuerpo) throws Exception {
        return mockMvc.perform(post(RUTA + accion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    // ---------- CA-050 ----------

    @Test
    @DisplayName("CA-050 POST block de una reserva confirmada responde 204 y guarda el bloqueo")
    void bloqueaReserva() throws Exception {
        enviar("/block", bloqueo(500, dia(5), dia(8)))
                .andExpect(status().isNoContent());

        assertTrue(bloqueoRepository.findByIdReserva(500L).isPresent());
    }

    @Test
    @DisplayName("Decisión 3: un reintento del mismo bloqueo responde 204 sin duplicarlo")
    void reintentoIdempotente() throws Exception {
        enviar("/block", bloqueo(500, dia(5), dia(8))).andExpect(status().isNoContent());
        enviar("/block", bloqueo(500, dia(5), dia(8))).andExpect(status().isNoContent());

        assertEquals(1, bloqueoRepository.count());
    }

    // ---------- CA-052 ----------

    @Test
    @DisplayName("CA-052 POST block con fechas que se cruzan con otra reserva responde 409")
    void rechazaReservaCruzada() throws Exception {
        enviar("/block", bloqueo(500, dia(5), dia(8))).andExpect(status().isNoContent());

        enviar("/block", bloqueo(501, dia(7), dia(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("Las fechas se cruzan con otra reserva confirmada"));

        assertEquals(1, bloqueoRepository.count());
    }

    @Test
    @DisplayName("CA-052 POST check devuelve solo las publicaciones con las fechas libres")
    void checkDevuelveDisponibles() throws Exception {
        periodoRepository.save(new PeriodoDisponible(16L, dia(1), dia(20)));
        enviar("/block", bloqueo(500, dia(5), dia(8))).andExpect(status().isNoContent());

        enviar("/check", """
                {"publicacionIds": [15, 16, 17], "fechaLlegada": "%s", "fechaSalida": "%s"}
                """.formatted(dia(6), dia(9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibles.length()").value(1))
                .andExpect(jsonPath("$.disponibles[0]").value(16));
    }

    @Test
    @DisplayName("Decisión 2: POST block fuera de los periodos disponibles responde 409")
    void rechazaFueraDePeriodos() throws Exception {
        enviar("/block", bloqueo(500, dia(18), dia(25)))
                .andExpect(status().isConflict());
    }

    // ---------- Decisión 4 ----------

    @Test
    @DisplayName("Decisión 4: POST release libera las fechas y responde 204")
    void liberaReserva() throws Exception {
        enviar("/block", bloqueo(500, dia(5), dia(8))).andExpect(status().isNoContent());

        enviar("/release", bloqueo(500, dia(5), dia(8)))
                .andExpect(status().isNoContent());

        assertEquals(0, bloqueoRepository.count());
    }

    @Test
    @DisplayName("Decisión 4: POST release de una reserva sin bloqueo también responde 204")
    void liberaSinBloqueo() throws Exception {
        enviar("/release", bloqueo(999, dia(5), dia(8)))
                .andExpect(status().isNoContent());
    }

    // ---------- Validaciones ----------

    @Test
    @DisplayName("POST block sin reserva responde 400")
    void rechazaSinReserva() throws Exception {
        enviar("/block", """
                {"publicacionId": 15, "fechaLlegada": "%s", "fechaSalida": "%s"}
                """.formatted(dia(5), dia(8)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La reserva es obligatoria"));
    }

    @Test
    @DisplayName("POST block con salida no posterior a la llegada responde 400")
    void rechazaFechasInvalidas() throws Exception {
        enviar("/block", bloqueo(500, dia(8), dia(5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST check sin lista de publicaciones responde 400")
    void checkSinPublicaciones() throws Exception {
        enviar("/check", """
                {"fechaLlegada": "%s", "fechaSalida": "%s"}
                """.formatted(dia(5), dia(8)))
                .andExpect(status().isBadRequest());
    }
}
