package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MisPublicacionesControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String URL = "/listings/mine";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PublicacionRepository repositorio;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        repositorio.deleteAll();
        when(serviciosExternos.verificarAnfitrion(TOKEN)).thenReturn(7L);
    }

    private Publicacion guardar(Long anfitrionId, String titulo) {
        Publicacion publicacion =
                new Publicacion(anfitrionId, LocalDateTime.of(2026, 11, 17, 10, 0));

        publicacion.definirDatosBasicos(
                titulo,
                "Alojamiento de prueba",
                "Bogota",
                "Centro",
                TipoAlojamiento.CASA,
                4,
                new BigDecimal("150000")
        );

        return repositorio.save(publicacion);
    }

    @Test
    @DisplayName("CA-043: el anfitrion consulta todas sus publicaciones")
    void consultaSusPublicaciones() throws Exception {
        guardar(7L, "Casa uno");
        guardar(7L, "Casa dos");

        mockMvc.perform(get(URL).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].titulo").value("Casa uno"))
                .andExpect(jsonPath("$.items[1].titulo").value("Casa dos"))
                .andExpect(jsonPath("$.items[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$.items[1].estado").value("BORRADOR"));
    }

    @Test
    @DisplayName("CA-044: no aparecen publicaciones de otros anfitriones")
    void noConsultaPublicacionesAjenas() throws Exception {
        guardar(7L, "Mi casa");
        guardar(99L, "Casa de otro anfitrion");

        mockMvc.perform(get(URL).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].titulo").value("Mi casa"))
                .andExpect(jsonPath("$.items[0].anfitrionId").value(7));
    }

    @Test
    @DisplayName("HU-15: sin publicaciones devuelve lista vacia")
    void devuelveListaVacia() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("HU-15: sin token devuelve 401")
    void rechazaSinAutorizacion() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }
}