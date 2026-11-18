package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.IntegracionesBloqueo;
import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.exception.AutenticacionException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BloqueoPublicacionControllerTest {

    private static final String TOKEN = "Bearer token-admin";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PublicacionRepository repositorio;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    @MockitoBean
    private IntegracionesBloqueo integracionesBloqueo;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        repositorio.deleteAll();

        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenReturn(1L);
    }

    private String url(Long id) {
        return "/admin/listings/" + id + "/block";
    }

    private Publicacion crearPublicacion() {
        Publicacion publicacion =
                new Publicacion(7L, LocalDateTime.now());

        publicacion.definirDatosBasicos(
                "Cabana en el lago",
                "Alojamiento con vista al lago",
                "Guatavita",
                "Vereda del lago",
                TipoAlojamiento.CABANA,
                4,
                new BigDecimal("250000"));

        return repositorio.save(publicacion);
    }

    @Test
    void administradorBloqueaPublicacion() throws Exception {
        Publicacion publicacion = crearPublicacion();

        mockMvc.perform(post(url(publicacion.getId()))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.motivoBloqueo")
                        .value("Incumple las reglas"));

        Publicacion guardada =
                repositorio.findById(publicacion.getId()).orElseThrow();

        assertEquals("BLOQUEADA", guardada.getEstado().name());
    }

    @Test
    void rechazaMotivoCorto() throws Exception {
        Publicacion publicacion = crearPublicacion();

        mockMvc.perform(post(url(publicacion.getId()))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"ab\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicacionInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post(url(999999L))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioSinPermisoRecibe403() throws Exception {
        doThrow(new PermisoException("Solo un administrador puede bloquear publicaciones"))
                .when(serviciosExternos).verificarAdministrador(TOKEN);

        mockMvc.perform(post(url(999999L))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenInvalidoRecibe401() throws Exception {
        doThrow(new AutenticacionException("Sesion invalida"))
                .when(serviciosExternos).verificarAdministrador(TOKEN);

        mockMvc.perform(post(url(999999L))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicacionBloqueadaDesapareceDelCatalogo() throws Exception {
        Publicacion publicacion = new Publicacion(7L, LocalDateTime.now());

        publicacion.definirDatosBasicos(
                "Cabana activa",
                "Alojamiento con vista al lago",
                "Guatavita",
                "Vereda del lago",
                TipoAlojamiento.CABANA,
                4,
                new BigDecimal("250000"));

        ReflectionTestUtils.setField(
                publicacion, "estado", EstadoPublicacion.ACTIVA);

        publicacion = repositorio.save(publicacion);
        Long id = publicacion.getId();

        mockMvc.perform(get("/interno/publicaciones/activas")
                .param("ciudad", "Guatavita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(post(url(id))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));

        mockMvc.perform(get("/interno/publicaciones/activas")
                .param("ciudad", "Guatavita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        Publicacion guardada = repositorio.findById(id).orElseThrow();

        assertEquals(EstadoPublicacion.BLOQUEADA, guardada.getEstado());
        assertEquals("Incumple las reglas", guardada.getMotivoBloqueo());
    }

    @Test
    void bloqueoSolicitaNotificacionYAuditoria() throws Exception {
        Publicacion publicacion = crearPublicacion();

        mockMvc.perform(post(url(publicacion.getId()))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isOk());

        verify(integracionesBloqueo).notificarBloqueo(
                7L,
                publicacion.getId(),
                "Incumple las reglas");

        verify(integracionesBloqueo).registrarAuditoria(
                1L,
                publicacion.getId(),
                "Incumple las reglas");
    }
    @Test
    void publicacionInexistenteNoGeneraEventos() throws Exception {
        mockMvc.perform(post(url(999999L))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(integracionesBloqueo);
    }
    @Test
    void bloqueoSolicitaRevisionManualDeReservas() throws Exception {
        Publicacion publicacion = crearPublicacion();

        mockMvc.perform(post(url(publicacion.getId()))
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Incumple las reglas\"}"))
                .andExpect(status().isOk());

        verify(integracionesBloqueo).solicitarRevisionManual(
                publicacion.getId());
    }
}