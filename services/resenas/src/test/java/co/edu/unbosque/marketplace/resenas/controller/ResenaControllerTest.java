package co.edu.unbosque.marketplace.resenas.controller;

import co.edu.unbosque.marketplace.resenas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.PermisoException;
import co.edu.unbosque.marketplace.resenas.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-32, como las haría el frontend.
// Identidad y Reservas se simulan con ServiciosExternos
@SpringBootTest
class ResenaControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final Long HUESPED = 21L;
    private static final Long OTRO_HUESPED = 99L;
    private static final Long RESERVA = 300L;
    private static final Long PUBLICACION = 15L;
    private static final String URL = "/bookings/300/review";
    private static final String MENSAJE_CALIFICACION = "La calificación debe ser un número entero entre 1 y 5";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ResenaRepository resenaRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        resenaRepository.deleteAll();
        // El token es del huésped 21 y la reserva 300 es suya y está COMPLETADA
        when(serviciosExternos.verificarHuesped(TOKEN)).thenReturn(HUESPED);
        reservasResponde("COMPLETADA", HUESPED);
    }

    private void reservasResponde(String estado, Long huespedId) {
        when(serviciosExternos.consultarReserva(TOKEN, RESERVA))
                .thenReturn(new ReservaDTO(RESERVA, PUBLICACION, huespedId, estado));
    }

    private ResultActions resenar(String cuerpo) throws Exception {
        return mockMvc.perform(post(URL).header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---------- CA-088 ----------

    @Test
    @DisplayName("CA-088 la reseña de una reserva COMPLETADA propia queda publicada (201)")
    void creaResena() throws Exception {
        resenar("{\"calificacion\":5,\"contenido\":\"Excelente lugar\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reservaId").value(300))
                .andExpect(jsonPath("$.huespedId").value(21))
                .andExpect(jsonPath("$.publicacionId").value(15))
                .andExpect(jsonPath("$.calificacion").value(5))
                .andExpect(jsonPath("$.contenido").value("Excelente lugar"))
                .andExpect(jsonPath("$.visible").value(true))
                .andExpect(jsonPath("$.fecha").exists());

        assertEquals(1, resenaRepository.count());
    }

    @Test
    @DisplayName("Decisión 5: el huésped sale del token aunque el cuerpo traiga otro huespedId")
    void ignoraHuespedDelCuerpo() throws Exception {
        resenar("{\"calificacion\":4,\"contenido\":\"Bien\",\"huespedId\":99,\"publicacionId\":77}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.huespedId").value(21))
                .andExpect(jsonPath("$.publicacionId").value(15));
    }

    // ---------- CA-089 ----------

    @Test
    @DisplayName("CA-089 una reserva CONFIRMADA todavía no se puede reseñar (409)")
    void rechazaNoCompletada() throws Exception {
        reservasResponde("CONFIRMADA", HUESPED);

        resenar("{\"calificacion\":4,\"contenido\":\"Aún no viajo\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo se puede reseñar una estancia completada"));
        assertEquals(0, resenaRepository.count());
    }

    // ---------- CA-090 ----------

    @Test
    @DisplayName("CA-090 la misma reserva no se puede reseñar dos veces (409)")
    void rechazaDuplicada() throws Exception {
        resenar("{\"calificacion\":5,\"contenido\":\"Primera\"}").andExpect(status().isCreated());

        resenar("{\"calificacion\":2,\"contenido\":\"Segunda\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("Esta reserva ya tiene una reseña"));
        assertEquals(1, resenaRepository.count());
    }

    // ---------- CA-091 y decisión 1 ----------

    @ParameterizedTest(name = "calificación {0}")
    @ValueSource(strings = {"0", "6", "-2", "4.5", "null"})
    @DisplayName("CA-091 una calificación fuera de 1 a 5, decimal o ausente se rechaza (400)")
    void rechazaCalificacionInvalida(String calificacion) throws Exception {
        resenar("{\"calificacion\":" + calificacion + ",\"contenido\":\"Texto\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_CALIFICACION));

        assertEquals(0, resenaRepository.count());
        // Se rechaza antes de consultar a Identidad y Reservas
        verify(serviciosExternos, never()).consultarReserva(anyString(), anyLong());
    }

    @Test
    @DisplayName("Decisión 1: sin contenido se rechaza (400)")
    void rechazaSinContenido() throws Exception {
        resenar("{\"calificacion\":4,\"contenido\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El contenido de la reseña es obligatorio"));
    }

    @Test
    @DisplayName("Decisión 1: un contenido de más de 1000 caracteres se rechaza (400)")
    void rechazaContenidoLargo() throws Exception {
        resenar("{\"calificacion\":4,\"contenido\":\"" + "a".repeat(1001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El contenido de la reseña no puede superar 1000 caracteres"));
    }

    @Test
    @DisplayName("Un JSON mal formado se rechaza (400)")
    void rechazaJsonMalFormado() throws Exception {
        resenar("{\"calificacion\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }

    @Test
    @DisplayName("Un id de reserva que no es número se rechaza (400)")
    void rechazaIdNoNumerico() throws Exception {
        mockMvc.perform(post("/bookings/abc/review").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacion\":4,\"contenido\":\"Texto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }

    // ---------- Decisión 2: solo reservas propias ----------

    @Test
    @DisplayName("Decisión 2: la reserva de otro huésped responde 404")
    void rechazaReservaAjena() throws Exception {
        reservasResponde("COMPLETADA", OTRO_HUESPED);

        resenar("{\"calificacion\":5,\"contenido\":\"No es mía\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La reserva no existe"));
    }

    @Test
    @DisplayName("Decisión 2: si Reservas no encuentra la reserva se responde 404")
    void reservaNoExiste() throws Exception {
        when(serviciosExternos.consultarReserva(TOKEN, RESERVA))
                .thenThrow(new NoEncontradoException("La reserva no existe"));

        resenar("{\"calificacion\":5,\"contenido\":\"Texto\"}")
                .andExpect(status().isNotFound());
    }

    // ---------- Sesión y rol ----------

    @Test
    @DisplayName("Sin el encabezado Authorization se responde 401")
    void sinToken() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacion\":4,\"contenido\":\"Texto\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("Con un token inválido se responde 401")
    void tokenInvalido() throws Exception {
        when(serviciosExternos.verificarHuesped(TOKEN)).thenThrow(new AutenticacionException("Debe iniciar sesión"));

        resenar("{\"calificacion\":4,\"contenido\":\"Texto\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un usuario que no es huésped recibe 403")
    void noEsHuesped() throws Exception {
        when(serviciosExternos.verificarHuesped(TOKEN))
                .thenThrow(new PermisoException("Solo un huésped puede reseñar una estancia"));

        resenar("{\"calificacion\":4,\"contenido\":\"Texto\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo un huésped puede reseñar una estancia"));
    }

    // ---------- Decisión 6 ----------

    @Test
    @DisplayName("Decisión 6: si Reservas no responde se responde 503")
    void reservasCaido() throws Exception {
        when(serviciosExternos.consultarReserva(TOKEN, RESERVA))
                .thenThrow(new ServicioNoDisponibleException("No fue posible consultar la reserva. Intente más tarde"));

        resenar("{\"calificacion\":4,\"contenido\":\"Texto\"}")
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, resenaRepository.count());
    }
}
