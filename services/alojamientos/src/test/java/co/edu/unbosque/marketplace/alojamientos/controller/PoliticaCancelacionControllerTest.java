package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import co.edu.unbosque.marketplace.alojamientos.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PoliticaCancelacionControllerTest {

    private static final String TOKEN = "Bearer token-prueba";
    private static final Long ANFITRION = 7L;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private PoliticaCancelacionRepository politicaRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;
    private PoliticaCancelacion politica;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        publicacionRepository.deleteAll();
        politicaRepository.deleteAll();

        // Datos de prueba, no son políticas oficiales del negocio.
        politica = politicaRepository.save(new PoliticaCancelacion(
                "Politica de prueba",
                "Devolucion definida para verificar la HU-11",
                new BigDecimal("50.00"),
                true));

        when(serviciosExternos.verificarAnfitrion(
                eq(TOKEN), anyString())).thenReturn(ANFITRION);
    }

    private Publicacion crearPublicacion(Long anfitrionId) {
        Publicacion publicacion = new Publicacion(
                anfitrionId, LocalDateTime.of(2026, 11, 17, 10, 0));

        publicacion.definirDatosBasicos(
                "Cabana de prueba",
                "Alojamiento para pruebas",
                "Guatavita",
                "Zona rural",
                TipoAlojamiento.CABANA,
                4,
                new BigDecimal("250000"));

        return publicacionRepository.save(publicacion);
    }

    private void asociar(Long id, Long politicaId) throws Exception {
        mockMvc.perform(put("/listings/" + id + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":" + politicaId + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void listaPoliticasDisponibles() throws Exception {
        mockMvc.perform(get("/cancellation-policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].nombre").value("Politica de prueba"))
                .andExpect(jsonPath("$.items[0].porcentajeDevolucion").value(50.00));
    }

    @Test
    void anfitrionAsociaPolitica() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        mockMvc.perform(put("/listings/" + id + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":" + politica.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politicaCancelacionId")
                        .value(politica.getId().intValue()));

        Publicacion guardada = publicacionRepository.findById(id).orElseThrow();

        assertEquals(politica.getId(),
                guardada.getPoliticaCancelacion().getId());
    }

    @Test
    void otroAnfitrionNoPuedeAsociar() throws Exception {
        Long id = crearPublicacion(99L).getId();

        mockMvc.perform(put("/listings/" + id + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":" + politica.getId() + "}"))
                .andExpect(status().isForbidden());

        assertNull(publicacionRepository.findById(id)
                .orElseThrow().getPoliticaCancelacion());
    }

    @Test
    void politicaInexistenteSeRechaza() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        mockMvc.perform(put("/listings/" + id + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":999999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void politicaSeMuestraEnDetalle() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        asociar(id, politica.getId());

        mockMvc.perform(get("/interno/publicaciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politicaCancelacion")
                        .value("Politica de prueba"));
    }

    @Test
    void reservasPuedeConsultarPolitica() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        asociar(id, politica.getId());

        mockMvc.perform(get("/internal/listings/" + id + "/cancellation-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(politica.getId().intValue()))
                .andExpect(jsonPath("$.porcentajeDevolucion").value(50.00))
                .andExpect(jsonPath("$.permiteLiberacionPeriodo").value(true));
    }

    @Test
    void publicacionSinPoliticaDevuelve404EnConsultaInterna() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        mockMvc.perform(get("/internal/listings/" + id + "/cancellation-policy"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicacionInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/listings/999999/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":" + politica.getId() + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicacionBloqueadaNoPermiteModificarPolitica() throws Exception {
        Publicacion publicacion = crearPublicacion(ANFITRION);

        ReflectionTestUtils.setField(
                publicacion, "estado", EstadoPublicacion.BLOQUEADA);

        publicacionRepository.save(publicacion);

        mockMvc.perform(put("/listings/" + publicacion.getId() + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":" + politica.getId() + "}"))
                .andExpect(status().isConflict());

        assertNull(publicacionRepository.findById(publicacion.getId())
                .orElseThrow().getPoliticaCancelacion());
    }

    @Test
    void politicaConIdNegativoSeRechaza() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        mockMvc.perform(put("/listings/" + id + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"politicaId\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitudSinPoliticaSeRechaza() throws Exception {
        Long id = crearPublicacion(ANFITRION).getId();

        mockMvc.perform(put("/listings/" + id + "/cancellation-policy")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}