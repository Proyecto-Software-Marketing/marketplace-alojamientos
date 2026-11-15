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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de GET /internal/reviews/averages como la llamaría el servicio Búsqueda (HU-33, decisión 6)
@SpringBootTest
class ResenaInternaControllerTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 15, 10, 0);

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
    }

    private Resena resena(Long reservaId, Long publicacionId, int calificacion) {
        return resenaRepository.save(new Resena(reservaId, publicacionId, 21L, calificacion, "Reseña", HOY));
    }

    @Test
    @DisplayName("Decisión 6: responde el mapa publicación → promedio sin reseñas ocultas y sin token")
    void promedios() throws Exception {
        resena(300L, 15L, 5);
        resena(301L, 15L, 4);
        Resena ocultada = resena(302L, 15L, 1);
        ocultada.ocultar(1L, "Incumple las políticas", HOY);
        resenaRepository.save(ocultada);
        resena(303L, 16L, 3);

        mockMvc.perform(get("/internal/reviews/averages").param("listingIds", "15,16,999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['15']").value(4.5))
                .andExpect(jsonPath("$['16']").value(3.0))
                .andExpect(jsonPath("$['999']").doesNotExist());

        verifyNoInteractions(serviciosExternos);
    }

    @Test
    @DisplayName("Decisión 6: sin el parámetro listingIds se rechaza (400)")
    void sinParametro() throws Exception {
        mockMvc.perform(get("/internal/reviews/averages"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Falta el parámetro obligatorio: listingIds"));
    }

    @Test
    @DisplayName("Decisión 6: con un id que no es número se rechaza (400)")
    void idInvalido() throws Exception {
        mockMvc.perform(get("/internal/reviews/averages").param("listingIds", "15,abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El id de publicación no es válido: 'abc'"));
    }

    @Test
    @DisplayName("Decisión 6: con listingIds vacío se rechaza (400)")
    void listaVacia() throws Exception {
        mockMvc.perform(get("/internal/reviews/averages").param("listingIds", ""))
                .andExpect(status().isBadRequest());
    }
}
