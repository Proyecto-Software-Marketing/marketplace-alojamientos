package co.edu.unbosque.marketplace.resenas.controller;

import co.edu.unbosque.marketplace.resenas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-33, como las haría el frontend en el detalle de la publicación
@SpringBootTest
class ConsultaResenasControllerTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 15, 10, 0);

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ResenaRepository resenaRepository;

    // Identidad y Reservas simulados: la consulta pública no debe llamarlos
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        resenaRepository.deleteAll();
    }

    private Resena resena(Long reservaId, Long publicacionId, int calificacion, String contenido, int horasAtras) {
        return resenaRepository.save(new Resena(reservaId, publicacionId, 21L, calificacion, contenido,
                HOY.minusHours(horasAtras)));
    }

    @Test
    @DisplayName("CA-092 y CA-093 sin iniciar sesión veo las reseñas visibles, la más reciente primero, y el promedio")
    void listaResenasVisibles() throws Exception {
        resena(300L, 15L, 5, "Excelente", 5);
        resena(301L, 15L, 4, "Muy bien", 1);
        Resena ocultada = resena(302L, 15L, 1, "Ofensiva", 0);
        ocultada.ocultar();
        resenaRepository.save(ocultada);
        resena(303L, 16L, 2, "Otra publicación", 0);

        mockMvc.perform(get("/listings/15/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionPromedio").value(4.5))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].reservaId").value(301))
                .andExpect(jsonPath("$.items[0].publicacionId").value(15))
                .andExpect(jsonPath("$.items[0].calificacion").value(4))
                .andExpect(jsonPath("$.items[0].contenido").value("Muy bien"))
                .andExpect(jsonPath("$.items[0].visible").value(true))
                .andExpect(jsonPath("$.items[0].fecha").exists())
                .andExpect(jsonPath("$.items[1].reservaId").value(300));

        // Decisión 1: es pública, no se consulta a Identidad
        verifyNoInteractions(serviciosExternos);
    }

    @Test
    @DisplayName("Decisiones 3 y 5: una publicación sin reseñas responde 200 con lista vacía y promedio null")
    void sinResenas() throws Exception {
        mockMvc.perform(get("/listings/999/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionPromedio").value(nullValue()))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("Decisión 5: un id de publicación que no es número se rechaza (400)")
    void idNoNumerico() throws Exception {
        mockMvc.perform(get("/listings/abc/reviews"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }
}
