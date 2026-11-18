
package co.edu.unbosque.marketplace.alojamientos.controller;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class EditarPublicacionControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PublicacionRepository repositorio;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;
    private Long publicacionId;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        repositorio.deleteAll();

        Publicacion publicacion = new Publicacion(
                7L, LocalDateTime.now());

        publicacion.definirDatosBasicos(
                "Titulo original",
                "Descripcion original",
                "Bogota",
                "Centro",
                co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento.CASA,
                2,
                new java.math.BigDecimal("150000"));

        publicacionId = repositorio.save(publicacion).getId();
    }

    private String datosEditados() {
        return """
                {
                  "titulo": "Titulo actualizado",
                  "descripcion": "Descripcion actualizada",
                  "ciudad": "Medellin",
                  "ubicacionTextual": "El Poblado",
                  "tipoAlojamiento": "CASA",
                  "capacidad": 4,
                  "precioPorNoche": 250000
                }
                """;
    }

    @Test
    void ca023AnfitrionPropietarioPuedeEditar() throws Exception {
        when(serviciosExternos.verificarEditor(TOKEN))
                .thenReturn(new UsuarioIdentidadDTO(7L, "ANFITRION"));

        mockMvc.perform(put("/listings/" + publicacionId)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datosEditados()))
                .andExpect(status().isOk());

        Publicacion guardada = repositorio.findById(publicacionId).orElseThrow();
        assertEquals("Titulo actualizado", guardada.getTitulo());
    }

    @Test
    void ca024AdministradorPuedeEditarYGeneraAuditoria() throws Exception {
        when(serviciosExternos.verificarEditor(TOKEN))
                .thenReturn(new UsuarioIdentidadDTO(10L, "ADMINISTRADOR"));

        mockMvc.perform(put("/listings/" + publicacionId)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datosEditados()))
                .andExpect(status().isOk());

        Publicacion guardada = repositorio.findById(publicacionId).orElseThrow();
        assertEquals("Titulo actualizado", guardada.getTitulo());

        verify(serviciosExternos)
                .registrarAuditoriaEdicion(10L, publicacionId);
    }

    @Test
    void ca025AnfitrionNoPropietarioEsRechazado() throws Exception {
        when(serviciosExternos.verificarEditor(TOKEN))
                .thenReturn(new UsuarioIdentidadDTO(99L, "ANFITRION"));

        mockMvc.perform(put("/listings/" + publicacionId)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datosEditados()))
                .andExpect(status().isForbidden());

        Publicacion guardada = repositorio.findById(publicacionId).orElseThrow();
        assertEquals("Titulo original", guardada.getTitulo());

        verify(serviciosExternos, never())
                .registrarAuditoriaEdicion(99L, publicacionId);
    }

    @Test
    void ca023RechazaDatosInvalidos() throws Exception {
        when(serviciosExternos.verificarEditor(TOKEN))
                .thenReturn(new UsuarioIdentidadDTO(7L, "ANFITRION"));

        String datosInvalidos = datosEditados()
                .replace("\"capacidad\": 4", "\"capacidad\": 0");

        mockMvc.perform(put("/listings/" + publicacionId)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datosInvalidos))
                .andExpect(status().isBadRequest());

        Publicacion guardada = repositorio.findById(publicacionId).orElseThrow();
        assertEquals("Titulo original", guardada.getTitulo());
    }

    @Test
    void ca024ConservaEdicionSiAuditoriaFalla() throws Exception {
        when(serviciosExternos.verificarEditor(TOKEN))
                .thenReturn(new UsuarioIdentidadDTO(10L, "ADMINISTRADOR"));

        doReturn(false).when(serviciosExternos)
                .registrarAuditoriaEdicion(10L, publicacionId);

        mockMvc.perform(put("/listings/" + publicacionId)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datosEditados()))
                .andExpect(status().isOk());

        Publicacion guardada = repositorio.findById(publicacionId).orElseThrow();
        assertEquals("Titulo actualizado", guardada.getTitulo());

        verify(serviciosExternos)
                .registrarAuditoriaEdicion(10L, publicacionId);
    }

}
