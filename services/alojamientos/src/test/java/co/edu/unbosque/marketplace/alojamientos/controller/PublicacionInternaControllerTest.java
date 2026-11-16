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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de las rutas /interno/publicaciones como las llaman Búsqueda, Reportes e Identidad
@SpringBootTest
class PublicacionInternaControllerTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Long ANFITRION = 7L;
    private static final Long OTRO_ANFITRION = 8L;

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
    private Publicacion publicacion(Long anfitrionId, String titulo, String ciudad, EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion(anfitrionId, HOY);
        publicacion.definirDatosBasicos(titulo, "Con vista al lago", ciudad, "Vereda Tominé, km 3",
                TipoAlojamiento.CABANA, 4, new BigDecimal("250000"));
        ReflectionTestUtils.setField(publicacion, "estado", estado);
        return publicacionRepository.save(publicacion);
    }

    private EstadoPublicacion estadoDe(Publicacion publicacion) {
        return publicacionRepository.findById(publicacion.getId()).orElseThrow().getEstado();
    }

    // ---------- Decisión 12: GET /interno/publicaciones/activas ----------

    @Test
    @DisplayName("Decisión 12: responde un arreglo con las publicaciones ACTIVA de la ciudad, sin token")
    void activasDeLaCiudad() throws Exception {
        Publicacion cabana = publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);
        publicacion(ANFITRION, "Apartamento central", "Bogotá", EstadoPublicacion.ACTIVA);
        publicacion(ANFITRION, "Cabaña en borrador", "Guatavita", EstadoPublicacion.BORRADOR);
        publicacion(OTRO_ANFITRION, "Cabaña pausada", "Guatavita", EstadoPublicacion.PAUSADA);

        // Los nombres de los campos son los de PublicacionResumen en Búsqueda
        mockMvc.perform(get("/interno/publicaciones/activas").param("ciudad", "Guatavita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(cabana.getId().intValue()))
                .andExpect(jsonPath("$[0].titulo").value("Cabaña en el lago"))
                .andExpect(jsonPath("$[0].ciudad").value("Guatavita"))
                .andExpect(jsonPath("$[0].tipo").value("CABANA"))
                .andExpect(jsonPath("$[0].capacidad").value(4))
                .andExpect(jsonPath("$[0].precioNoche").value(250000))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"))
                .andExpect(jsonPath("$[0].servicios").isArray());

        verifyNoInteractions(serviciosExternos);
    }

    @ParameterizedTest(name = "ciudad \"{0}\"")
    @ValueSource(strings = {"guatavita", "GUATAVITA", "  Guatavita "})
    @DisplayName("Decisión 12: la ciudad se compara sin distinguir mayúsculas ni espacios sobrantes")
    void activasSinDistinguirMayusculas(String ciudad) throws Exception {
        publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);

        mockMvc.perform(get("/interno/publicaciones/activas").param("ciudad", ciudad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Decisión 12: sin ciudad responde todas las publicaciones ACTIVA, de la más antigua a la más nueva")
    void activasSinCiudad() throws Exception {
        Publicacion primera = publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);
        Publicacion segunda = publicacion(OTRO_ANFITRION, "Apartamento central", "Bogotá",
                EstadoPublicacion.ACTIVA);
        publicacion(ANFITRION, "En revisión", "Bogotá", EstadoPublicacion.PENDIENTE_REVISION);

        mockMvc.perform(get("/interno/publicaciones/activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(primera.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(segunda.getId().intValue()));
    }

    @Test
    @DisplayName("Decisión 12: si no hay publicaciones activas en la ciudad responde un arreglo vacío")
    void activasSinResultados() throws Exception {
        publicacion(ANFITRION, "Cabaña en borrador", "Guatavita", EstadoPublicacion.BORRADOR);

        mockMvc.perform(get("/interno/publicaciones/activas").param("ciudad", "Guatavita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- Decisión 13: GET /interno/publicaciones/{id} ----------

    @Test
    @DisplayName("Decisión 13: responde el detalle con los nombres que espera Búsqueda")
    void detalle() throws Exception {
        Publicacion cabana = publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);

        mockMvc.perform(get("/interno/publicaciones/" + cabana.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cabana.getId().intValue()))
                .andExpect(jsonPath("$.titulo").value("Cabaña en el lago"))
                .andExpect(jsonPath("$.ciudad").value("Guatavita"))
                .andExpect(jsonPath("$.tipo").value("CABANA"))
                .andExpect(jsonPath("$.capacidad").value(4))
                .andExpect(jsonPath("$.precioNoche").value(250000))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.descripcion").value("Con vista al lago"))
                .andExpect(jsonPath("$.servicios").isArray())
                .andExpect(jsonPath("$.reglas").isArray())
                .andExpect(jsonPath("$.imagenes").isArray())
                .andExpect(jsonPath("$.politicaCancelacion").doesNotExist());

        verifyNoInteractions(serviciosExternos);
    }

    @Test
    @DisplayName("Decisión 13: el detalle se responde en cualquier estado (Búsqueda filtra las ACTIVA)")
    void detalleEnBorrador() throws Exception {
        Publicacion borrador = publicacion(ANFITRION, "Cabaña en borrador", "Guatavita",
                EstadoPublicacion.BORRADOR);

        mockMvc.perform(get("/interno/publicaciones/" + borrador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @DisplayName("Decisión 13: si la publicación no existe se responde 404")
    void detalleNoExiste() throws Exception {
        mockMvc.perform(get("/interno/publicaciones/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no existe"));
    }

    @Test
    @DisplayName("Decisión 13: un id que no es un número se rechaza (400)")
    void detalleConIdInvalido() throws Exception {
        mockMvc.perform(get("/interno/publicaciones/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }

    // ---------- Decisión 14: GET /interno/publicaciones/estadisticas ----------

    @Test
    @DisplayName("Decisión 14: responde cuántas publicaciones están ACTIVA")
    void estadisticas() throws Exception {
        publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);
        publicacion(OTRO_ANFITRION, "Apartamento central", "Bogotá", EstadoPublicacion.ACTIVA);
        publicacion(ANFITRION, "Cabaña en borrador", "Guatavita", EstadoPublicacion.BORRADOR);
        publicacion(ANFITRION, "Cabaña bloqueada", "Guatavita", EstadoPublicacion.BLOQUEADA);

        mockMvc.perform(get("/interno/publicaciones/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activas").value(2));
    }

    @Test
    @DisplayName("Decisión 14: sin publicaciones responde cero")
    void estadisticasSinPublicaciones() throws Exception {
        mockMvc.perform(get("/interno/publicaciones/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activas").value(0));
    }

    // ---------- Decisión 15: POST /interno/publicaciones/pausar ----------

    @Test
    @DisplayName("Decisión 15: las publicaciones ACTIVA del anfitrión pasan a PAUSADA (204)")
    void pausaLasActivasDelAnfitrion() throws Exception {
        // Dado un anfitrión con dos publicaciones ACTIVA y otro anfitrión con una
        Publicacion primera = publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);
        Publicacion segunda = publicacion(ANFITRION, "Apartamento central", "Bogotá", EstadoPublicacion.ACTIVA);
        Publicacion ajena = publicacion(OTRO_ANFITRION, "Casa campestre", "Chía", EstadoPublicacion.ACTIVA);

        // Cuando Identidad pide pausar las del primero
        mockMvc.perform(post("/interno/publicaciones/pausar").param("idAnfitrion", "7"))
                .andExpect(status().isNoContent());

        // Entonces solo las suyas quedan PAUSADA
        assertEquals(EstadoPublicacion.PAUSADA, estadoDe(primera));
        assertEquals(EstadoPublicacion.PAUSADA, estadoDe(segunda));
        assertEquals(EstadoPublicacion.ACTIVA, estadoDe(ajena));
        verifyNoInteractions(serviciosExternos);
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"BORRADOR", "PENDIENTE_REVISION", "PAUSADA", "BLOQUEADA"})
    @DisplayName("Decisión 15: las publicaciones que no están ACTIVA conservan su estado")
    void noCambiaLasQueNoEstanActivas(EstadoPublicacion estado) throws Exception {
        Publicacion publicacion = publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", estado);

        mockMvc.perform(post("/interno/publicaciones/pausar").param("idAnfitrion", "7"))
                .andExpect(status().isNoContent());

        assertEquals(estado, estadoDe(publicacion));
    }

    @Test
    @DisplayName("Decisión 15: repetir la llamada o pausar a un anfitrión sin publicaciones también responde 204")
    void pausarEsIdempotente() throws Exception {
        Publicacion activa = publicacion(ANFITRION, "Cabaña en el lago", "Guatavita", EstadoPublicacion.ACTIVA);

        mockMvc.perform(post("/interno/publicaciones/pausar").param("idAnfitrion", "7"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/interno/publicaciones/pausar").param("idAnfitrion", "7"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/interno/publicaciones/pausar").param("idAnfitrion", "12345"))
                .andExpect(status().isNoContent());

        assertEquals(EstadoPublicacion.PAUSADA, estadoDe(activa));
    }

    @Test
    @DisplayName("Decisión 15: sin el parámetro idAnfitrion se rechaza (400)")
    void pausarSinAnfitrion() throws Exception {
        mockMvc.perform(post("/interno/publicaciones/pausar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Falta el parámetro obligatorio: idAnfitrion"));
    }

    @Test
    @DisplayName("Decisión 15: un idAnfitrion que no es un número se rechaza (400)")
    void pausarConAnfitrionInvalido() throws Exception {
        mockMvc.perform(post("/interno/publicaciones/pausar").param("idAnfitrion", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }
}
