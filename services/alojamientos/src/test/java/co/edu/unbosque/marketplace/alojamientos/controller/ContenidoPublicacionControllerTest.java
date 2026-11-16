package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.exception.AutenticacionException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-09, como las haría el frontend, y de cómo se ven los cambios
// en las rutas que consume Búsqueda (detalle y filtro). Identidad se simula con ServiciosExternos
@SpringBootTest
class ContenidoPublicacionControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String SIN_PERMISO = "Solo el anfitrión puede modificar sus publicaciones";
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
        // El token es del anfitrión 7
        when(serviciosExternos.verificarAnfitrion(eq(TOKEN), anyString())).thenReturn(ANFITRION);
    }

    // La aprobación llega con la HU-13; mientras tanto las pruebas fijan el estado directamente
    private Publicacion publicacion(Long anfitrionId, EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion(anfitrionId, HOY);
        publicacion.definirDatosBasicos("Cabaña en el lago", "Con vista al lago", "Guatavita",
                "Vereda Tominé, km 3", TipoAlojamiento.CABANA, 4, new BigDecimal("250000"));
        ReflectionTestUtils.setField(publicacion, "estado", estado);
        return publicacionRepository.save(publicacion);
    }

    private Publicacion guardada(Long id) {
        return publicacionRepository.findById(id).orElseThrow();
    }

    private ResultActions cambiar(Long id, String ruta, String cuerpo) throws Exception {
        return mockMvc.perform(put("/listings/" + id + "/" + ruta).header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---------- CA-026: servicios ----------

    @Test
    @DisplayName("CA-026 el anfitrión agrega servicios y la respuesta trae la publicación completa (200)")
    void agregaServicios() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "amenities", "{\"servicios\":[\"piscina\",\"Wifi\",\"wifi\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.anfitrionId").value(7))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.titulo").value("Cabaña en el lago"))
                .andExpect(jsonPath("$.servicios.length()").value(2))
                .andExpect(jsonPath("$.servicios[0]").value("Wifi"))
                .andExpect(jsonPath("$.servicios[1]").value("Piscina"))
                .andExpect(jsonPath("$.reglas").isEmpty())
                .andExpect(jsonPath("$.imagenes").isEmpty());

        assertEquals(List.of("Wifi", "Piscina"), guardada(id).getServicios());
    }

    @Test
    @DisplayName("CA-026 quitar servicios se refleja en el detalle que consume Búsqueda")
    void quitaServiciosSeVeEnElDetalle() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.ACTIVA).getId();
        cambiar(id, "amenities", "{\"servicios\":[\"Wifi\",\"Cocina\"]}").andExpect(status().isOk());

        // Cuando quito Cocina
        cambiar(id, "amenities", "{\"servicios\":[\"Wifi\"]}").andExpect(status().isOk());

        // Entonces el detalle solo muestra Wifi
        mockMvc.perform(get("/interno/publicaciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios.length()").value(1))
                .andExpect(jsonPath("$.servicios[0]").value("Wifi"));
    }

    @Test
    @DisplayName("CA-026 los servicios llegan a Búsqueda en las publicaciones activas para usarlos como filtro")
    void serviciosEnLasActivas() throws Exception {
        Long conPiscina = publicacion(ANFITRION, EstadoPublicacion.ACTIVA).getId();
        Long sinServicios = publicacion(ANFITRION, EstadoPublicacion.ACTIVA).getId();
        cambiar(conPiscina, "amenities", "{\"servicios\":[\"PISCINA\",\"wifi\"]}").andExpect(status().isOk());

        // Búsqueda compara los textos exactos del catálogo ("Piscina", "Wifi")
        mockMvc.perform(get("/interno/publicaciones/activas").param("ciudad", "Guatavita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(conPiscina.intValue()))
                .andExpect(jsonPath("$[0].servicios[0]").value("Wifi"))
                .andExpect(jsonPath("$[0].servicios[1]").value("Piscina"))
                .andExpect(jsonPath("$[1].id").value(sinServicios.intValue()))
                .andExpect(jsonPath("$[1].servicios").isEmpty());
    }

    @Test
    @DisplayName("Decisión 5: un servicio fuera del catálogo se rechaza indicando los válidos (400)")
    void rechazaServicioFueraDelCatalogo() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "amenities", "{\"servicios\":[\"Wifi\",\"Jacuzzi\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value("Cada servicio debe ser uno de: Wifi, Cocina, "
                        + "Aire acondicionado, Lavadora, Aparcamiento, Piscina"));
        assertTrue(guardada(id).getServicios().isEmpty());
    }

    // ---------- CA-027: reglas ----------

    @Test
    @DisplayName("CA-027 las reglas registradas se ven en la respuesta y en el detalle")
    void registraReglas() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "rules", "{\"reglas\":[\"No fumar\",\" No se admiten mascotas \"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reglas.length()").value(2))
                .andExpect(jsonPath("$.reglas[0]").value("No fumar"))
                .andExpect(jsonPath("$.reglas[1]").value("No se admiten mascotas"));

        mockMvc.perform(get("/interno/publicaciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reglas[0]").value("No fumar"))
                .andExpect(jsonPath("$.reglas[1]").value("No se admiten mascotas"));
    }

    @Test
    @DisplayName("Decisión 8: una regla vacía se rechaza (400)")
    void rechazaReglaVacia() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "rules", "{\"reglas\":[\"No fumar\",\"  \"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Las reglas no pueden estar vacías"));
    }

    // ---------- CA-028: imágenes ----------

    @Test
    @DisplayName("CA-028 las referencias de imágenes se ven en la respuesta y en el detalle, en orden")
    void registraImagenes() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "images", "{\"imagenes\":[\"https://fotos.ejemplo.com/cabana-1.jpg\",\"/imagenes/cabana-2.png\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenes.length()").value(2))
                .andExpect(jsonPath("$.imagenes[0]").value("https://fotos.ejemplo.com/cabana-1.jpg"))
                .andExpect(jsonPath("$.imagenes[1]").value("/imagenes/cabana-2.png"));

        mockMvc.perform(get("/interno/publicaciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenes[0]").value("https://fotos.ejemplo.com/cabana-1.jpg"))
                .andExpect(jsonPath("$.imagenes[1]").value("/imagenes/cabana-2.png"));
    }

    @Test
    @DisplayName("Decisión 11: una referencia de imagen sin formato válido se rechaza (400)")
    void rechazaImagenInvalida() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "images", "{\"imagenes\":[\"cabana.jpg\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Cada imagen debe ser una dirección que empiece por "
                        + "http://, https:// o / y no tenga espacios"));
    }

    // ---------- Decisión 2: la lista es obligatoria ----------

    @ParameterizedTest(name = "{0} con cuerpo {1}")
    @CsvSource(delimiter = '|', value = {
            "amenities | {}                     | La lista de servicios es obligatoria",
            "amenities | {\"servicios\":null}   | La lista de servicios es obligatoria",
            "rules     | {}                     | La lista de reglas es obligatoria",
            "rules     | {\"reglas\":null}      | La lista de reglas es obligatoria",
            "images    | {}                     | La lista de imágenes es obligatoria",
            "images    | {\"imagenes\":null}    | La lista de imágenes es obligatoria"})
    @DisplayName("Decisión 2: sin la lista o con la lista en null se rechaza (400)")
    void rechazaSinLista(String ruta, String cuerpo, String mensaje) throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, ruta, cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value(mensaje));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "amenities | {\"servicios\":[]}",
            "rules     | {\"reglas\":[]}",
            "images    | {\"imagenes\":[]}"})
    @DisplayName("Decisión 2: una lista vacía deja la publicación sin elementos (200)")
    void listaVaciaQuitaTodo(String ruta, String cuerpo) throws Exception {
        Publicacion publicacion = publicacion(ANFITRION, EstadoPublicacion.BORRADOR);
        publicacion.definirServicios(List.of("Wifi"));
        publicacion.definirReglas(List.of("No fumar"));
        publicacion.definirImagenes(List.of("/img/a.jpg"));
        Long id = publicacionRepository.save(publicacion).getId();

        cambiar(id, ruta, cuerpo).andExpect(status().isOk());

        Publicacion despues = guardada(id);
        int total = despues.getServicios().size() + despues.getReglas().size() + despues.getImagenes().size();
        assertEquals(2, total);
    }

    @Test
    @DisplayName("Decisión 2: un cuerpo que no es JSON válido se rechaza (400)")
    void rechazaJsonMalFormado() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(id, "amenities", "{\"servicios\":\"Wifi\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }

    // ---------- Decisiones 1, 3 y 4: permisos, dueño y estado ----------

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "amenities | {\"servicios\":[\"Wifi\"]}",
            "rules     | {\"reglas\":[\"No fumar\"]}",
            "images    | {\"imagenes\":[\"/img/a.jpg\"]}"})
    @DisplayName("Decisión 1: un huésped no puede modificar la publicación (403)")
    void rechazaHuesped(String ruta, String cuerpo) throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();
        when(serviciosExternos.verificarAnfitrion(eq(TOKEN), anyString()))
                .thenThrow(new PermisoException(SIN_PERMISO));

        cambiar(id, ruta, cuerpo)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value(SIN_PERMISO));
        verify(serviciosExternos).verificarAnfitrion(TOKEN, SIN_PERMISO);
    }

    @Test
    @DisplayName("Decisión 1: sin el encabezado Authorization se responde 401")
    void rechazaSinToken() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        mockMvc.perform(put("/listings/" + id + "/rules").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reglas\":[\"No fumar\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
        verify(serviciosExternos, never()).verificarAnfitrion(anyString(), anyString());
    }

    @Test
    @DisplayName("Decisión 1: con el token inválido se responde 401 y con Identidad caída 503")
    void tokenInvalidoEIdentidadCaida() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();

        // doThrow porque el método ya quedó configurado para lanzar una excepción
        doThrow(new AutenticacionException("Debe iniciar sesión"))
                .when(serviciosExternos).verificarAnfitrion(eq(TOKEN), anyString());
        cambiar(id, "images", "{\"imagenes\":[\"/img/a.jpg\"]}").andExpect(status().isUnauthorized());

        doThrow(new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde"))
                .when(serviciosExternos).verificarAnfitrion(eq(TOKEN), anyString());
        cambiar(id, "images", "{\"imagenes\":[\"/img/a.jpg\"]}").andExpect(status().isServiceUnavailable());

        assertTrue(guardada(id).getImagenes().isEmpty());
    }

    @Test
    @DisplayName("Decisión 3: la publicación de otro anfitrión o una que no existe responde 404")
    void publicacionAjenaONoExiste() throws Exception {
        Long ajena = publicacion(OTRO_ANFITRION, EstadoPublicacion.BORRADOR).getId();

        cambiar(ajena, "amenities", "{\"servicios\":[\"Wifi\"]}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no existe"));
        cambiar(999999L, "amenities", "{\"servicios\":[\"Wifi\"]}")
                .andExpect(status().isNotFound());

        assertTrue(guardada(ajena).getServicios().isEmpty());
    }

    @Test
    @DisplayName("Decisión 3: un id que no es un número se rechaza (400)")
    void idInvalido() throws Exception {
        mockMvc.perform(put("/listings/abc/amenities").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"servicios\":[\"Wifi\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"PENDIENTE_REVISION", "BLOQUEADA"})
    @DisplayName("Decisión 4: en PENDIENTE_REVISION y BLOQUEADA se responde 409")
    void rechazaEstadoNoEditable(EstadoPublicacion estado) throws Exception {
        Long id = publicacion(ANFITRION, estado).getId();

        cambiar(id, "rules", "{\"reglas\":[\"No fumar\"]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo se pueden cambiar los servicios, reglas e "
                        + "imágenes de una publicación en BORRADOR, ACTIVA o PAUSADA"));
        assertTrue(guardada(id).getReglas().isEmpty());
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"ACTIVA", "PAUSADA"})
    @DisplayName("Decisión 4: en ACTIVA y PAUSADA se puede modificar y el estado no cambia")
    void permiteActivaYPausada(EstadoPublicacion estado) throws Exception {
        Long id = publicacion(ANFITRION, estado).getId();

        cambiar(id, "images", "{\"imagenes\":[\"/img/a.jpg\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(estado.name()))
                .andExpect(jsonPath("$.imagenes[0]").value("/img/a.jpg"));
    }
}
