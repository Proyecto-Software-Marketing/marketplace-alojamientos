package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.FavoritoRepository;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.identidad.service.SesionService;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
class FavoritoControllerTest {

    private static final String CLAVE = "clave12345";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionService sesionService;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mvc;
    private String tokenHuesped;
    private String tokenAnfitrion;


    @BeforeEach
    void prepararDatos() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        favoritoRepository.deleteAll();
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.registrar(new RegistroRequest("Ana Pérez", "ana@correo.com", CLAVE, Rol.HUESPED));
        usuarioService.registrar(new RegistroRequest("Luis Gómez", "luis@correo.com", CLAVE, Rol.ANFITRION));

        tokenHuesped = "Bearer " + sesionService.iniciarSesion(new LoginRequest("ana@correo.com", CLAVE)).getToken();
        tokenAnfitrion = "Bearer " + sesionService.iniciarSesion(new LoginRequest("luis@correo.com", CLAVE)).getToken();
    }

    @Test
    @DisplayName("CA-121 al marcar una publicación aparece en mis favoritos")
    void agregarFavorito() throws Exception {
        mvc.perform(post("/usuarios/favoritos/5").header("Authorization", tokenHuesped))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicaciones[0]").value(5));

        mvc.perform(get("/usuarios/favoritos").header("Authorization", tokenHuesped))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicaciones.length()").value(1));
    }

    @Test
    @DisplayName("CA-121 si la marco dos veces no se repite")
    void noRepiteFavorito() throws Exception {
        mvc.perform(post("/usuarios/favoritos/5").header("Authorization", tokenHuesped));
        mvc.perform(post("/usuarios/favoritos/5").header("Authorization", tokenHuesped))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicaciones.length()").value(1));
    }

    @Test
    @DisplayName("CA-122 al quitar una publicación deja de aparecer")
    void quitarFavorito() throws Exception {
        mvc.perform(post("/usuarios/favoritos/5").header("Authorization", tokenHuesped));
        mvc.perform(post("/usuarios/favoritos/7").header("Authorization", tokenHuesped));

        mvc.perform(delete("/usuarios/favoritos/5").header("Authorization", tokenHuesped))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicaciones.length()").value(1))
                .andExpect(jsonPath("$.publicaciones[0]").value(7));
    }

    @Test
    @DisplayName("Solo el huésped puede tener favoritos")
    void anfitrionNoTieneFavoritos() throws Exception {
        mvc.perform(get("/usuarios/favoritos").header("Authorization", tokenAnfitrion))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin sesión no se puede ver la lista")
    void sinSesion() throws Exception {
        mvc.perform(get("/usuarios/favoritos"))
                .andExpect(status().isUnauthorized());
    }
}