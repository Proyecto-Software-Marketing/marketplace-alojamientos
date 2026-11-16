package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.exception.AutenticacionException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-07, como las haría el frontend.
// Identidad se simula con ServiciosExternos
@SpringBootTest
class PublicacionControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final Long ANFITRION = 7L;
    private static final String URL = "/listings";
    private static final String MENSAJE_CAPACIDAD = "La capacidad debe ser un número entero entre 1 y 50";
    private static final String MENSAJE_PRECIO =
            "El precio por noche debe ser un valor entero en pesos mayor que cero y de máximo 9 dígitos";
    private static final String MENSAJE_TIPO =
            "El tipo de alojamiento debe ser uno de: APARTAMENTO, CASA, HABITACION, CABANA, OTRO";

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
        when(serviciosExternos.verificarAnfitrion(TOKEN)).thenReturn(ANFITRION);
    }

    // Cuerpo válido al que cada prueba le cambia el tipo, la capacidad y el precio (tal como irían en el JSON)
    private String cuerpo(String tipo, String capacidad, String precio) {
        return cuerpo("\"Cabaña en el lago\"", tipo, capacidad, precio);
    }

    private String cuerpo(String titulo, String tipo, String capacidad, String precio) {
        return "{\"titulo\":" + titulo + ",\"descripcion\":\"Cabaña de madera con vista al lago\","
                + "\"ciudad\":\"Guatavita\",\"ubicacionTextual\":\"Vereda Tominé, km 3\","
                + "\"tipoAlojamiento\":" + tipo + ",\"capacidad\":" + capacidad
                + ",\"precioPorNoche\":" + precio + "}";
    }

    private String cuerpoValido() {
        return cuerpo("\"CABANA\"", "4", "250000");
    }

    private ResultActions crear(String cuerpo) throws Exception {
        return mockMvc.perform(post(URL).header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---------- CA-020 ----------

    @Test
    @DisplayName("CA-020 el anfitrión crea la publicación y queda en BORRADOR asociada a él (201)")
    void creaPublicacion() throws Exception {
        crear(cuerpoValido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.anfitrionId").value(7))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.titulo").value("Cabaña en el lago"))
                .andExpect(jsonPath("$.descripcion").value("Cabaña de madera con vista al lago"))
                .andExpect(jsonPath("$.ciudad").value("Guatavita"))
                .andExpect(jsonPath("$.ubicacionTextual").value("Vereda Tominé, km 3"))
                .andExpect(jsonPath("$.tipoAlojamiento").value("CABANA"))
                .andExpect(jsonPath("$.capacidad").value(4))
                .andExpect(jsonPath("$.precioPorNoche").value(250000));

        assertEquals(1, publicacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 3: la respuesta trae servicios, reglas e imágenes como listas vacías")
    void respondeListasVacias() throws Exception {
        crear(cuerpoValido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servicios").isArray())
                .andExpect(jsonPath("$.servicios").isEmpty())
                .andExpect(jsonPath("$.reglas").isArray())
                .andExpect(jsonPath("$.reglas").isEmpty())
                .andExpect(jsonPath("$.imagenes").isArray())
                .andExpect(jsonPath("$.imagenes").isEmpty())
                .andExpect(jsonPath("$.motivoRechazo").doesNotExist());
    }

    @Test
    @DisplayName("Decisión 5: el anfitrión sale del token y el estado es BORRADOR aunque el cuerpo traiga otros")
    void ignoraAnfitrionYEstadoDelCuerpo() throws Exception {
        String conExtras = cuerpoValido().replace("{\"titulo\"",
                "{\"id\":999,\"anfitrionId\":99,\"estado\":\"ACTIVA\",\"titulo\"");

        crear(conExtras)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anfitrionId").value(7))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));

        Publicacion guardada = publicacionRepository.findAll().get(0);
        assertEquals(ANFITRION, guardada.getAnfitrionId());
        assertEquals(EstadoPublicacion.BORRADOR, guardada.getEstado());
    }

    // ---------- CA-021 ----------

    @ParameterizedTest(name = "capacidad {0}")
    @ValueSource(strings = {"0", "-1", "2.5", "51", "100", "null"})
    @DisplayName("CA-021 una capacidad menor o igual a cero, decimal, mayor a 50 o ausente se rechaza (400)")
    void rechazaCapacidadInvalida(String capacidad) throws Exception {
        crear(cuerpo("\"CABANA\"", capacidad, "250000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_CAPACIDAD));
        assertEquals(0, publicacionRepository.count());
    }

    @ParameterizedTest(name = "precio {0}")
    @ValueSource(strings = {"0", "-1", "-250000", "1500.5", "1234567890", "null"})
    @DisplayName("CA-021 un precio que no es positivo, con decimales, de más de 9 dígitos o ausente se rechaza (400)")
    void rechazaPrecioInvalido(String precio) throws Exception {
        crear(cuerpo("\"CABANA\"", "4", precio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_PRECIO));
        assertEquals(0, publicacionRepository.count());
    }

    @Test
    @DisplayName("CA-021 con capacidad y precio inválidos se indican los dos errores")
    void indicaTodosLosErrores() throws Exception {
        crear(cuerpo("\"CABANA\"", "0", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(2))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_PRECIO))
                .andExpect(jsonPath("$.mensajes[1]").value(MENSAJE_CAPACIDAD));
    }

    // ---------- CA-022 y decisión 4 ----------

    @Test
    @DisplayName("CA-022 un huésped no puede crear una publicación (403)")
    void rechazaHuesped() throws Exception {
        // Dado que soy huésped: Identidad no autoriza el rol ANFITRION
        when(serviciosExternos.verificarAnfitrion(TOKEN))
                .thenThrow(new PermisoException("Solo un anfitrión puede crear publicaciones"));

        // Cuando intento crear una publicación, entonces el sistema no lo permite
        crear(cuerpoValido())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo un anfitrión puede crear publicaciones"));
        assertEquals(0, publicacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 4: sin el encabezado Authorization se responde 401")
    void rechazaSinToken() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpoValido()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));

        verify(serviciosExternos, never()).verificarAnfitrion(anyString());
        assertEquals(0, publicacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 4: con el token vencido o inválido se responde 401")
    void rechazaTokenInvalido() throws Exception {
        when(serviciosExternos.verificarAnfitrion(TOKEN)).thenThrow(new AutenticacionException("Debe iniciar sesión"));

        crear(cuerpoValido())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
        assertEquals(0, publicacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 4: si Identidad no responde se responde 503 y no se guarda nada")
    void identidadCaida() throws Exception {
        when(serviciosExternos.verificarAnfitrion(TOKEN)).thenThrow(
                new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde"));

        crear(cuerpoValido())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes[0]").value("No fue posible validar la sesión. Intente más tarde"));
        assertEquals(0, publicacionRepository.count());
    }

    // ---------- Decisiones 6 y 7 ----------

    @Test
    @DisplayName("Decisión 6: sin ningún dato se indica cada campo obligatorio (400)")
    void rechazaCuerpoVacio() throws Exception {
        crear("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(7))
                // Los mensajes salen en orden alfabético
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_PRECIO))
                .andExpect(jsonPath("$.mensajes[1]").value(MENSAJE_TIPO))
                .andExpect(jsonPath("$.mensajes[2]").value("El título es obligatorio"))
                .andExpect(jsonPath("$.mensajes[3]").value(MENSAJE_CAPACIDAD))
                .andExpect(jsonPath("$.mensajes[4]").value("La ciudad es obligatoria"))
                .andExpect(jsonPath("$.mensajes[5]").value("La descripción es obligatoria"))
                .andExpect(jsonPath("$.mensajes[6]").value("La ubicación es obligatoria"));
        assertEquals(0, publicacionRepository.count());
    }

    @ParameterizedTest(name = "título {0}")
    @ValueSource(strings = {"\"\"", "\"   \"", "null"})
    @DisplayName("Decisión 6: un título vacío, en blanco o ausente se rechaza (400)")
    void rechazaTituloVacio(String titulo) throws Exception {
        crear(cuerpo(titulo, "\"CABANA\"", "4", "250000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El título es obligatorio"));
    }

    @Test
    @DisplayName("Decisión 6: un título de más de 120 caracteres se rechaza (400)")
    void rechazaTituloLargo() throws Exception {
        crear(cuerpo("\"" + "a".repeat(121) + "\"", "\"CABANA\"", "4", "250000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El título no puede superar 120 caracteres"));
    }

    @Test
    @DisplayName("Decisión 6: se acepta un título de exactamente 120 caracteres")
    void aceptaTituloEnElLimite() throws Exception {
        crear(cuerpo("\"" + "a".repeat(120) + "\"", "\"CABANA\"", "4", "250000"))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "tipo {0}")
    @ValueSource(strings = {"\"HOTEL\"", "\"cabana\"", "\"\"", "null"})
    @DisplayName("Decisión 7: un tipo de alojamiento fuera de la lista se rechaza indicando los válidos (400)")
    void rechazaTipoInvalido(String tipo) throws Exception {
        crear(cuerpo(tipo, "4", "250000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_TIPO));
    }

    @ParameterizedTest(name = "tipo {0}, capacidad {1}")
    @CsvSource({"APARTAMENTO, 1", "CASA, 50", "HABITACION, 2", "OTRO, 10"})
    @DisplayName("Decisiones 7 y 8: se aceptan los tipos del contrato y los extremos de la capacidad")
    void aceptaTiposYExtremos(String tipo, int capacidad) throws Exception {
        crear(cuerpo("\"" + tipo + "\"", String.valueOf(capacidad), "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoAlojamiento").value(tipo))
                .andExpect(jsonPath("$.capacidad").value(capacidad));
    }

    // ---------- Formato ----------

    @ParameterizedTest(name = "cuerpo {0}")
    @ValueSource(strings = {"{\"titulo\":", "no es json"})
    @DisplayName("Decisión 10: un JSON mal formado se rechaza con el formato de errores del equipo (400)")
    void rechazaJsonMalFormado(String cuerpo) throws Exception {
        crear(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }

    @Test
    @DisplayName("Decisión 10: una capacidad que no es un número se rechaza (400)")
    void rechazaCapacidadConTexto() throws Exception {
        crear(cuerpo("\"CABANA\"", "\"cuatro\"", "250000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }
}
