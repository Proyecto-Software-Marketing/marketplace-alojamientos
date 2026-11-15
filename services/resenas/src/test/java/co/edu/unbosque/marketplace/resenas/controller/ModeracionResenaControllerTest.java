package co.edu.unbosque.marketplace.resenas.controller;

import co.edu.unbosque.marketplace.resenas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.resenas.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.resenas.exception.AutenticacionException;
import co.edu.unbosque.marketplace.resenas.exception.PermisoException;
import co.edu.unbosque.marketplace.resenas.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-36, como las haría el panel de administración.
// Identidad y Auditoría se simulan con ServiciosExternos
@SpringBootTest
class ModeracionResenaControllerTest {

    private static final String TOKEN = "Bearer token-admin";
    private static final Long ADMIN = 1L;
    private static final LocalDateTime AYER = LocalDateTime.of(2026, 11, 14, 10, 0);

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ResenaRepository resenaRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;
    private Resena ofensiva;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        resenaRepository.deleteAll();
        when(serviciosExternos.verificarAdministrador(TOKEN)).thenReturn(ADMIN);
        when(serviciosExternos.registrarAuditoria(any())).thenReturn(true);
        resenaRepository.save(new Resena(300L, 15L, 21L, 5, "Excelente", AYER));
        ofensiva = resenaRepository.save(new Resena(301L, 15L, 22L, 1, "Texto ofensivo", AYER));
    }

    private ResultActions ocultar(Long resenaId, String cuerpo) throws Exception {
        return mockMvc.perform(post("/admin/reviews/" + resenaId + "/hide").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---------- CA-100 ----------

    @Test
    @DisplayName("CA-100 la reseña oculta deja de mostrarse en la publicación y no cuenta en el promedio")
    void ocultaResena() throws Exception {
        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ofensiva.getId()))
                .andExpect(jsonPath("$.visible").value(false));

        // La consulta pública de HU-33 ya no la muestra ni la cuenta
        mockMvc.perform(get("/listings/15/reviews"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].reservaId").value(300))
                .andExpect(jsonPath("$.calificacionPromedio").value(5.0));
    }

    // ---------- CA-101 ----------

    @Test
    @DisplayName("CA-101 se envía a Auditoría quién la ocultó, cuál y por qué")
    void enviaEventoDeAuditoria() throws Exception {
        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}").andExpect(status().isOk());

        ArgumentCaptor<EventoAuditoriaDTO> evento = ArgumentCaptor.forClass(EventoAuditoriaDTO.class);
        verify(serviciosExternos).registrarAuditoria(evento.capture());
        assertEquals(new EventoAuditoriaDTO(ADMIN, "RESENA", ofensiva.getId(), "OCULTAR_RESENA", "EXITO",
                "Lenguaje ofensivo"), evento.getValue());
    }

    @Test
    @DisplayName("Decisión 6: si Auditoría no responde la reseña igual queda oculta (200)")
    void auditoriaCaida() throws Exception {
        when(serviciosExternos.registrarAuditoria(any())).thenReturn(false);

        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

        Resena guardada = resenaRepository.findById(ofensiva.getId()).orElseThrow();
        assertFalse(guardada.isVisible());
        assertEquals(ADMIN, guardada.getOcultadaPor());
    }

    // ---------- Decisión 4 ----------

    @Test
    @DisplayName("Decisión 4: ocultar de nuevo responde 200 sin enviar otro evento a Auditoría")
    void ocultarDosVeces() throws Exception {
        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}").andExpect(status().isOk());

        ocultar(ofensiva.getId(), "{\"motivo\":\"Otro motivo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

        verify(serviciosExternos, times(1)).registrarAuditoria(any());
        assertEquals("Lenguaje ofensivo",
                resenaRepository.findById(ofensiva.getId()).orElseThrow().getMotivoOcultamiento());
    }

    // ---------- Decisiones 2 y 3 ----------

    @Test
    @DisplayName("Decisión 2: un motivo de menos de 3 caracteres se rechaza (400)")
    void motivoCorto() throws Exception {
        ocultar(ofensiva.getId(), "{\"motivo\":\"ab\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El motivo es obligatorio y debe tener entre 3 y 500 caracteres"));

        assertTrue(resenaRepository.findById(ofensiva.getId()).orElseThrow().isVisible());
        verify(serviciosExternos, never()).registrarAuditoria(any());
    }

    @Test
    @DisplayName("Decisión 2: sin motivo se rechaza (400)")
    void sinMotivo() throws Exception {
        ocultar(ofensiva.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1));
    }

    @Test
    @DisplayName("Decisión 2: un motivo de más de 500 caracteres se rechaza (400)")
    void motivoLargo() throws Exception {
        ocultar(ofensiva.getId(), "{\"motivo\":\"" + "a".repeat(501) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Decisión 3: si la reseña no existe se responde 404")
    void resenaNoExiste() throws Exception {
        ocultar(9999L, "{\"motivo\":\"Lenguaje ofensivo\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La reseña no existe"));
    }

    @Test
    @DisplayName("Un id de reseña que no es número se rechaza (400)")
    void idNoNumerico() throws Exception {
        mockMvc.perform(post("/admin/reviews/abc/hide").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Lenguaje ofensivo\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Decisión 1: sesión y rol ----------

    @Test
    @DisplayName("Decisión 1: sin el encabezado Authorization se responde 401")
    void sinToken() throws Exception {
        mockMvc.perform(post("/admin/reviews/" + ofensiva.getId() + "/hide")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Lenguaje ofensivo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Decisión 1: con un token inválido se responde 401")
    void tokenInvalido() throws Exception {
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenThrow(new AutenticacionException("Debe iniciar sesión"));

        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Decisión 1: un usuario que no es administrador recibe 403 y la reseña sigue visible")
    void noEsAdministrador() throws Exception {
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenThrow(new PermisoException("Solo un administrador puede ocultar reseñas"));

        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo un administrador puede ocultar reseñas"));
        assertTrue(resenaRepository.findById(ofensiva.getId()).orElseThrow().isVisible());
    }

    @Test
    @DisplayName("Si Identidad no responde se responde 503 y la reseña sigue visible")
    void identidadCaida() throws Exception {
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenThrow(new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde"));

        ocultar(ofensiva.getId(), "{\"motivo\":\"Lenguaje ofensivo\"}")
                .andExpect(status().isServiceUnavailable());
        assertTrue(resenaRepository.findById(ofensiva.getId()).orElseThrow().isVisible());
    }
}
