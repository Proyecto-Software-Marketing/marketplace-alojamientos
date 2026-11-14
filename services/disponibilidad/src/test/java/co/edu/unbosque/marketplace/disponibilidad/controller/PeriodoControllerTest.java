package co.edu.unbosque.marketplace.disponibilidad.controller;

import co.edu.unbosque.marketplace.disponibilidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.disponibilidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.disponibilidad.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.PermisoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-16, como las haría el frontend
@SpringBootTest
class PeriodoControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String RUTA = "/listings/15/availability";
    private static final Long PUBLICACION = 15L;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PeriodoDisponibleRepository periodoRepository;

    @Autowired
    private BloqueoReservaRepository bloqueoRepository;

    // Doble de prueba: si no se programa, deja pasar (el usuario es el dueño)
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        bloqueoRepository.deleteAll();
        periodoRepository.deleteAll();
    }

    private LocalDate dia(int diasDesdeHoy) {
        return LocalDate.now().plusDays(diasDesdeHoy);
    }

    private String cuerpo(LocalDate inicio, LocalDate fin) {
        return """
                {"fechaInicio": "%s", "fechaFin": "%s"}
                """.formatted(inicio, fin);
    }

    private Long periodoGuardado(LocalDate inicio, LocalDate fin) {
        return periodoRepository.save(new PeriodoDisponible(PUBLICACION, inicio, fin)).getId();
    }

    // ---------- CA-045 ----------

    @Test
    @DisplayName("CA-045 POST con fechas válidas responde 201 y guarda el periodo")
    void registraPeriodo() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.idPublicacion").value(15))
                .andExpect(jsonPath("$.fechaInicio").value(dia(1).toString()))
                .andExpect(jsonPath("$.fechaFin").value(dia(10).toString()))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));

        assertEquals(1, periodoRepository.count());
    }

    // ---------- CA-046 ----------

    @Test
    @DisplayName("CA-046 POST con salida no posterior a la llegada responde 400 con el mensaje")
    void rechazaFechasInvalidas() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(10), dia(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("La fecha de salida debe ser posterior a la fecha de llegada"));

        assertEquals(0, periodoRepository.count());
    }

    @Test
    @DisplayName("POST sin fecha de salida responde 400")
    void rechazaFechaFaltante() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\": \"" + dia(1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La fecha de salida es obligatoria"));
    }

    @Test
    @DisplayName("POST con una fecha mal escrita responde 400")
    void rechazaFormatoInvalido() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\": \"mañana\", \"fechaFin\": \"pasado\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un id de publicación que no es número responde 400")
    void rechazaIdInvalido() throws Exception {
        mockMvc.perform(post("/listings/abc/availability")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(10))))
                .andExpect(status().isBadRequest());
    }

    // ---------- CA-047 ----------

    @Test
    @DisplayName("CA-047 DELETE de un periodo con reservas responde 409 y no lo borra")
    void noEliminaConReservas() throws Exception {
        Long id = periodoGuardado(dia(1), dia(20));
        bloqueoRepository.save(new BloqueoReserva(PUBLICACION, 500L, dia(5), dia(8)));

        mockMvc.perform(delete(RUTA + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isConflict());

        assertTrue(periodoRepository.existsById(id));
    }

    @Test
    @DisplayName("CA-047 PUT que recorta un periodo con reservas responde 409")
    void noRecortaConReservas() throws Exception {
        Long id = periodoGuardado(dia(1), dia(20));
        bloqueoRepository.save(new BloqueoReserva(PUBLICACION, 500L, dia(5), dia(8)));

        mockMvc.perform(put(RUTA + "/" + id)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(6))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT válido responde 200 con las nuevas fechas")
    void editaPeriodo() throws Exception {
        Long id = periodoGuardado(dia(1), dia(10));

        mockMvc.perform(put(RUTA + "/" + id)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(15))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value(dia(15).toString()));
    }

    @Test
    @DisplayName("DELETE de un periodo sin reservas responde 204")
    void eliminaPeriodo() throws Exception {
        Long id = periodoGuardado(dia(1), dia(10));

        mockMvc.perform(delete(RUTA + "/" + id).header("Authorization", TOKEN))
                .andExpect(status().isNoContent());

        assertEquals(0, periodoRepository.count());
    }

    // ---------- Permisos ----------

    @Test
    @DisplayName("Sin encabezado Authorization responde 401")
    void sinToken() throws Exception {
        mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(10))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("Con un token inválido responde 401")
    void tokenInvalido() throws Exception {
        doThrow(new AutenticacionException("Debe iniciar sesión"))
                .when(serviciosExternos).verificarDuenoPublicacion(anyString(), anyLong());

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(10))))
                .andExpect(status().isUnauthorized());

        assertEquals(0, periodoRepository.count());
    }

    @Test
    @DisplayName("HU-16 si no es el dueño de la publicación responde 403 y no guarda nada")
    void noEsElDueno() throws Exception {
        doThrow(new PermisoException("Solo el anfitrión dueño de la publicación puede gestionar su disponibilidad"))
                .when(serviciosExternos).verificarDuenoPublicacion(anyString(), anyLong());

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(10))))
                .andExpect(status().isForbidden());

        assertEquals(0, periodoRepository.count());
    }

    @Test
    @DisplayName("Si la publicación no existe responde 404")
    void publicacionNoExiste() throws Exception {
        doThrow(new NoEncontradoException("La publicación no existe"))
                .when(serviciosExternos).verificarDuenoPublicacion(anyString(), anyLong());

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(dia(1), dia(10))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Si Identidad o Alojamientos no responden se devuelve 503")
    void servicioExternoCaido() throws Exception {
        doThrow(new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde"))
                .when(serviciosExternos).verificarDuenoPublicacion(anyString(), anyLong());

        mockMvc.perform(delete(RUTA + "/1").header("Authorization", TOKEN))
                .andExpect(status().isServiceUnavailable());
    }
}