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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.alojamientos.dto.EventoAuditoriaDTO;
import static org.mockito.ArgumentMatchers.argThat;

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

// ---------- HU-12: Enviar publicacion a revision ----------

@Test
@DisplayName("CA-035: el anfitrion envia un borrador completo a revision (200)")
void enviaPublicacionARevision() throws Exception {
    crear(cuerpoValido()).andExpect(status().isCreated());

    Publicacion borrador = publicacionRepository.findAll().get(0);

    mockMvc.perform(post("/listings/{id}/submit", borrador.getId())
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION,
            publicacionRepository.findById(borrador.getId())
                    .orElseThrow().getEstado());
}

@Test
@DisplayName("HU-12: no permite enviar dos veces la misma publicacion (409)")
void rechazaSegundoEnvio() throws Exception {
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isConflict());
}

@Test
@DisplayName("HU-12: otro anfitrion no puede enviar la publicacion (403)")
void rechazaOtroAnfitrion() throws Exception {
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    when(serviciosExternos.verificarAnfitrion(TOKEN)).thenReturn(99L);

    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isForbidden());

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-12: una publicacion inexistente responde 404")
void rechazaPublicacionInexistente() throws Exception {
    mockMvc.perform(post("/listings/{id}/submit", 999999L)
                    .header("Authorization", TOKEN))
            .andExpect(status().isNotFound());
}

@Test
@DisplayName("CA-036: una publicacion incompleta responde 422 y sigue en BORRADOR")
void rechazaEnvioDePublicacionIncompleta() throws Exception {
    Publicacion incompleta = new Publicacion(
            ANFITRION, LocalDateTime.of(2026, 11, 16, 10, 30));

    incompleta.definirDatosBasicos(
            "", "", "Guatavita", "",
            TipoAlojamiento.CABANA, 4,
            BigDecimal.valueOf(250000));

    incompleta = publicacionRepository.save(incompleta);
    Long id = incompleta.getId();

    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.mensajes[0]")
                    .value(org.hamcrest.Matchers.containsString("titulo")))
            .andExpect(jsonPath("$.mensajes[0]")
                    .value(org.hamcrest.Matchers.containsString("descripcion")))
            .andExpect(jsonPath("$.mensajes[0]")
                    .value(org.hamcrest.Matchers.containsString("ubicacion")));

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(id)
                    .orElseThrow().getEstado());
}

    // ---------- HU-13: Aprobar publicaciones ----------

    @Test
    @DisplayName("CA-037: un administrador aprueba una publicacion pendiente (200)")
    void administradorApruebaPublicacion() throws Exception {
        // Crear una publicacion en borrador.
        crear(cuerpoValido()).andExpect(status().isCreated());

        Long id = publicacionRepository.findAll().get(0).getId();

        // El anfitrion envia la publicacion a revision.
        mockMvc.perform(post("/listings/{id}/submit", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());

        // Simular que Identidad reconoce al administrador.
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenReturn(10L);

        // El administrador aprueba la publicacion.
        mockMvc.perform(post("/admin/listings/{id}/approve", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // Verificar que el nuevo estado quedo guardado.
        assertEquals(EstadoPublicacion.ACTIVA,
                publicacionRepository.findById(id)
                        .orElseThrow().getEstado());

        verify(serviciosExternos).verificarAdministrador(TOKEN);
        // Comprobar que se solicita registrar la aprobacion.
verify(serviciosExternos).registrarAuditoria(
        argThat((EventoAuditoriaDTO evento) ->
                evento.usuarioId().equals(10L)
                        && evento.entidad().equals("PUBLICACION")
                        && evento.entidadId().equals(id)
                        && evento.accion().equals("APROBAR_PUBLICACION")
                        && evento.resultado().equals("EXITOSO")
        )
);

// Comprobar que se solicita notificar al anfitrion.
Long anfitrionId = publicacionRepository.findById(id)
        .orElseThrow()
        .getAnfitrionId();

verify(serviciosExternos).notificarPublicacionAprobada(
        anfitrionId,
        id
);
    }

    @Test
    @DisplayName("HU-13: un usuario sin rol administrador no puede aprobar (403)")
    void rechazaAprobacionSinPermisos() throws Exception {
        // Crear una publicacion.
        crear(cuerpoValido()).andExpect(status().isCreated());

        Long id = publicacionRepository.findAll().get(0).getId();

        // Enviar la publicacion a revision.
        mockMvc.perform(post("/listings/{id}/submit", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());

        // Simular que Identidad rechaza al usuario.
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenThrow(new PermisoException(
                        "Solo un administrador puede aprobar o rechazar publicaciones"));

        // Intentar aprobar sin permisos.
        mockMvc.perform(post("/admin/listings/{id}/approve", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isForbidden());

        // La publicacion debe continuar pendiente.
        assertEquals(EstadoPublicacion.PENDIENTE_REVISION,
                publicacionRepository.findById(id)
                        .orElseThrow().getEstado());
    }

    @Test
    @DisplayName("HU-13: aprobar una publicacion inexistente responde 404")
    void rechazaAprobacionDePublicacionInexistente() throws Exception {
        // Simular un administrador autorizado.
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenReturn(10L);

        // Intentar aprobar una publicacion que no existe.
        mockMvc.perform(post("/admin/listings/{id}/approve", 999999L)
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());

        // Comprobar que se verifico el rol administrador.
        verify(serviciosExternos).verificarAdministrador(TOKEN);

        // No debe haberse creado ninguna publicacion.
        assertEquals(0, publicacionRepository.count());
    }

    @Test
    @DisplayName("HU-13: no permite aprobar una publicacion en BORRADOR (409)")
    void rechazaAprobacionDeBorrador() throws Exception {
        // Crear una publicacion que permanece en BORRADOR.
        crear(cuerpoValido()).andExpect(status().isCreated());

        Long id = publicacionRepository.findAll().get(0).getId();

        // Simular un administrador autorizado.
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenReturn(10L);

        // Intentar aprobar sin haber enviado a revision.
        mockMvc.perform(post("/admin/listings/{id}/approve", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict());

        // Comprobar que la publicacion sigue en BORRADOR.
        assertEquals(EstadoPublicacion.BORRADOR,
                publicacionRepository.findById(id)
                        .orElseThrow().getEstado());
    }

    @Test
    @DisplayName("HU-13: no permite aprobar dos veces la misma publicacion (409)")
    void rechazaSegundaAprobacion() throws Exception {
        // Crear una publicacion.
        crear(cuerpoValido()).andExpect(status().isCreated());

        Long id = publicacionRepository.findAll().get(0).getId();

        // Enviar a revision.
        mockMvc.perform(post("/listings/{id}/submit", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());

        // Autorizar al administrador.
        when(serviciosExternos.verificarAdministrador(TOKEN))
                .thenReturn(10L);

        // Primera aprobacion exitosa.
        mockMvc.perform(post("/admin/listings/{id}/approve", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // Segunda aprobacion rechazada.
        mockMvc.perform(post("/admin/listings/{id}/approve", id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict());

        // El estado permanece ACTIVA.
        assertEquals(EstadoPublicacion.ACTIVA,
                publicacionRepository.findById(id)
                        .orElseThrow().getEstado());
    }
// ---------- HU-13: Rechazar publicaciones ----------

@Test
@DisplayName("CA-038: un administrador rechaza una publicacion con motivo (200)")
void administradorRechazaPublicacion() throws Exception {
    // Crear la publicacion.
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    // Enviarla a revision.
    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    // Autorizar al administrador.
    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    String motivo = "La publicacion no cumple con los requisitos";

    // Rechazar la publicacion.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"" + motivo + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("BORRADOR"))
            .andExpect(jsonPath("$.motivoRechazo").value(motivo));

    // Verificar que el rechazo quedo guardado.
    Publicacion guardada = publicacionRepository.findById(id)
            .orElseThrow();

    assertEquals(EstadoPublicacion.BORRADOR, guardada.getEstado());
    assertEquals(motivo, guardada.getMotivoRechazo());

    // Verificar la auditoria.
    verify(serviciosExternos).registrarAuditoria(
            argThat((EventoAuditoriaDTO evento) ->
                    evento.usuarioId().equals(10L)
                            && evento.entidadId().equals(id)
                            && evento.accion().equals("RECHAZAR_PUBLICACION")
                            && evento.resultado().equals("EXITOSO")
                            && evento.detalle().contains(motivo)
            )
    );
    // Verificar que se solicita notificar al anfitrion.
verify(serviciosExternos).notificarPublicacionRechazada(
        ANFITRION,
        id,
        motivo
);
}
@Test
@DisplayName("HU-13: un usuario sin rol administrador no puede rechazar (403)")
void rechazaPublicacionSinPermisos() throws Exception {
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenThrow(new PermisoException(
                    "Solo un administrador puede aprobar o rechazar publicaciones"));

    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"No cumple los requisitos\"}"))
            .andExpect(status().isForbidden());

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION,
            publicacionRepository.findById(id)
                    .orElseThrow().getEstado());

    verify(serviciosExternos, never()).registrarAuditoria(
            org.mockito.ArgumentMatchers.any(EventoAuditoriaDTO.class));
}
@Test
@DisplayName("HU-13: rechazar una publicacion inexistente responde 404")
void rechazaPublicacionInexistentePorAdministrador() throws Exception {
    // Simular un administrador autorizado.
    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    // Intentar rechazar una publicacion que no existe.
    mockMvc.perform(post("/admin/listings/{id}/reject", 999999L)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"No cumple los requisitos\"}"))
            .andExpect(status().isNotFound());

    // Verificar que no se registro una auditoria de rechazo exitoso.
    verify(serviciosExternos, never()).registrarAuditoria(
            org.mockito.ArgumentMatchers.any(EventoAuditoriaDTO.class));

    assertEquals(0, publicacionRepository.count());
}
@Test
@DisplayName("HU-13: no permite rechazar una publicacion en BORRADOR (409)")
void rechazaPublicacionEnBorrador() throws Exception {
    // Crear una publicacion que permanece en BORRADOR.
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    // Simular un administrador autorizado.
    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    // Intentar rechazar sin haber enviado a revision.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"No cumple los requisitos\"}"))
            .andExpect(status().isConflict());

    // La publicacion debe continuar en BORRADOR.
    Publicacion guardada = publicacionRepository.findById(id)
            .orElseThrow();

    assertEquals(EstadoPublicacion.BORRADOR, guardada.getEstado());
    org.junit.jupiter.api.Assertions.assertNull(
            guardada.getMotivoRechazo());

    // No debe registrarse una auditoria de rechazo exitoso.
    verify(serviciosExternos, never()).registrarAuditoria(
            org.mockito.ArgumentMatchers.any(EventoAuditoriaDTO.class));
}
@Test
@DisplayName("CA-038: no permite rechazar sin motivo (400)")
void rechazaPublicacionSinMotivo() throws Exception {
    // Crear una publicacion.
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    // Enviar la publicacion a revision.
    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    // Simular un administrador autorizado.
    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    // Intentar rechazar con el motivo vacio.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"\"}"))
            .andExpect(status().isBadRequest());

    // La publicacion debe continuar pendiente de revision.
    Publicacion guardada = publicacionRepository.findById(id)
            .orElseThrow();

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION,
            guardada.getEstado());

    org.junit.jupiter.api.Assertions.assertNull(
            guardada.getMotivoRechazo());

    // No debe registrarse una auditoria de rechazo exitoso.
    verify(serviciosExternos, never()).registrarAuditoria(
            org.mockito.ArgumentMatchers.any(EventoAuditoriaDTO.class));
}
@Test
@DisplayName("HU-13: no permite rechazar dos veces la misma publicacion (409)")
void rechazaSegundoRechazo() throws Exception {
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    // Enviar a revision.
    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    // Primer rechazo exitoso.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"No cumple los requisitos\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("BORRADOR"));

    // Segundo rechazo: debe responder 409.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"Otro motivo diferente\"}"))
            .andExpect(status().isConflict());

    // El primer rechazo debe mantenerse.
    Publicacion guardada = publicacionRepository.findById(id)
            .orElseThrow();

    assertEquals(EstadoPublicacion.BORRADOR, guardada.getEstado());
    assertEquals("No cumple los requisitos", guardada.getMotivoRechazo());
}
@Test
@DisplayName("CA-038: motivo de rechazo menor a 3 caracteres responde 400")
void rechazaMotivoDemasiadoCorto() throws Exception {
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    // Enviar a revision.
    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    // Intentar rechazar con un motivo de solo 2 caracteres.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"No\"}"))
            .andExpect(status().isBadRequest());

    // La publicacion debe continuar pendiente.
    Publicacion guardada = publicacionRepository.findById(id)
            .orElseThrow();

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION,
            guardada.getEstado());

    org.junit.jupiter.api.Assertions.assertNull(
            guardada.getMotivoRechazo());

    // No debe registrarse un rechazo exitoso.
    verify(serviciosExternos, never()).registrarAuditoria(
            org.mockito.ArgumentMatchers.any(EventoAuditoriaDTO.class));
}
@Test
@DisplayName("HU-13: rechaza motivos demasiado cortos aunque tengan espacios")
void rechazaMotivoCortoConEspacios() throws Exception {

    // Crear una publicacion.
    crear(cuerpoValido()).andExpect(status().isCreated());

    Long id = publicacionRepository.findAll().get(0).getId();

    // Enviar la publicacion a revision.
    mockMvc.perform(post("/listings/{id}/submit", id)
                    .header("Authorization", TOKEN))
            .andExpect(status().isOk());

    // Simular un administrador autorizado.
    when(serviciosExternos.verificarAdministrador(TOKEN))
            .thenReturn(10L);

    // Intentar rechazar con un motivo de un solo caracter y espacios.
    mockMvc.perform(post("/admin/listings/{id}/reject", id)
                    .header("Authorization", TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"  a  \"}"))
            .andExpect(status().isBadRequest());

    // Verificar que la publicacion siga pendiente.
    Publicacion guardada = publicacionRepository.findById(id)
            .orElseThrow();

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION,
            guardada.getEstado());

    org.junit.jupiter.api.Assertions.assertNull(
            guardada.getMotivoRechazo());

    // No debe registrarse auditoria ni enviarse notificacion.
    verify(serviciosExternos, never()).registrarAuditoria(
            org.mockito.ArgumentMatchers.any(EventoAuditoriaDTO.class));

    verify(serviciosExternos, never()).notificarPublicacionRechazada(
            org.mockito.ArgumentMatchers.any(Long.class),
            org.mockito.ArgumentMatchers.any(Long.class),
            org.mockito.ArgumentMatchers.any(String.class));
}
}
