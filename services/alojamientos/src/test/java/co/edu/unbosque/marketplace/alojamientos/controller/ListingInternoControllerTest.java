package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de GET /internal/listings/{id}/owner como la llaman Disponibilidad y Reservas
@SpringBootTest
class ListingInternoControllerTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Long ANFITRION = 7L;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        publicacionRepository.deleteAll();
    }

    // La aprobación llega con la HU-13; mientras tanto las pruebas fijan el estado directamente
    private Publicacion publicacion(EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion(ANFITRION, HOY);
        publicacion.definirDatosBasicos("Cabaña en el lago", "Con vista al lago", "Guatavita",
                "Vereda Tominé, km 3", TipoAlojamiento.CABANA, 4, new BigDecimal("250000"));
        ReflectionTestUtils.setField(publicacion, "estado", estado);
        return publicacionRepository.save(publicacion);
    }

    @Test
    @DisplayName("Decisión 11: responde publicacionId, anfitrionId y estado, sin token")
    void propietario() throws Exception {
        // Una publicación recién creada (BORRADOR): Disponibilidad ya puede verificar a su dueño
        Publicacion borrador = publicacion(EstadoPublicacion.BORRADOR);

        mockMvc.perform(get("/internal/listings/" + borrador.getId() + "/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicacionId").value(borrador.getId().intValue()))
                .andExpect(jsonPath("$.anfitrionId").value(7))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));

        verifyNoInteractions(serviciosExternos);
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(EstadoPublicacion.class)
    @DisplayName("Decisión 11: responde en cualquier estado, con el estado que tiene la publicación")
    void propietarioEnCualquierEstado(EstadoPublicacion estado) throws Exception {
        Publicacion publicacion = publicacion(estado);

        mockMvc.perform(get("/internal/listings/" + publicacion.getId() + "/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anfitrionId").value(7))
                .andExpect(jsonPath("$.estado").value(estado.name()));
    }

    @Test
    @DisplayName("Decisión 11: si la publicación no existe se responde 404")
    void propietarioNoExiste() throws Exception {
        mockMvc.perform(get("/internal/listings/999999/owner"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no existe"));
    }

    @Test
    @DisplayName("Decisión 11: un id que no es un número se rechaza (400)")
    void propietarioConIdInvalido() throws Exception {
        mockMvc.perform(get("/internal/listings/abc/owner"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }
}
