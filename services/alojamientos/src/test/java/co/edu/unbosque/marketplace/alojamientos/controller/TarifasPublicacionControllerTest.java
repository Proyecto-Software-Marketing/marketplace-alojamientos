package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-10 y de la ruta quote-data que consume Cotización.
// Identidad se simula con ServiciosExternos
@SpringBootTest
class TarifasPublicacionControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String SIN_PERMISO = "Solo el anfitrión puede modificar sus publicaciones";
    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Long ANFITRION = 7L;
    private static final Long OTRO_ANFITRION = 8L;

    private static final String MENSAJE_LIMPIEZA =
            "La tarifa de limpieza debe ser un valor entero en pesos, de 0 o más y de máximo 9 dígitos";
    private static final String MENSAJE_PORCENTAJE =
            "El porcentaje de descuento por estancia larga debe ser un número entero entre 0 y 90";
    private static final String MENSAJE_NOCHES =
            "Las noches mínimas para el descuento deben ser un número entero entre 2 y 365";
    private static final String MENSAJE_COSTO = "El costo de cada servicio adicional debe ser un valor entero en "
            + "pesos, mayor que cero y de máximo 9 dígitos";

    // Configuración completa: limpieza 60.000, 10 % desde 7 noches, desayuno y traslado
    private static final String TARIFAS_COMPLETAS = "{\"tarifaLimpieza\":60000,"
            + "\"porcentajeDescuentoEstanciaLarga\":10,\"nochesMinimasDescuento\":7,"
            + "\"serviciosAdicionales\":[{\"nombre\":\"Desayuno\",\"costo\":25000},"
            + "{\"nombre\":\"Traslado al aeropuerto\",\"costo\":80000}]}";

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

    private Publicacion publicacion(Long anfitrionId, EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion(anfitrionId, HOY);
        publicacion.definirDatosBasicos("Cabaña en el lago", "Con vista al lago", "Guatavita",
                "Vereda Tominé, km 3", TipoAlojamiento.CABANA, 4, new BigDecimal("250000"));
        ReflectionTestUtils.setField(publicacion, "estado", estado);
        return publicacionRepository.save(publicacion);
    }

    private Long borrador() {
        return publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();
    }

    private ResultActions definir(Long id, String cuerpo) throws Exception {
        return mockMvc.perform(put("/listings/" + id + "/pricing").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // Cuerpo con los valores tal como irían en el JSON
    private static String cuerpo(String limpieza, String porcentaje, String noches, String adicionales) {
        return "{\"tarifaLimpieza\":" + limpieza + ",\"porcentajeDescuentoEstanciaLarga\":" + porcentaje
                + ",\"nochesMinimasDescuento\":" + noches + ",\"serviciosAdicionales\":" + adicionales + "}";
    }

    // ---------- CA-029, CA-030 y CA-129 ----------

    @Test
    @DisplayName("CA-029, CA-030 y CA-129 el anfitrión define las tarifas y la respuesta trae la publicación (200)")
    void defineTarifas() throws Exception {
        Long id = borrador();

        definir(id, TARIFAS_COMPLETAS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.precioPorNoche").value(250000))
                .andExpect(jsonPath("$.tarifaLimpieza").value(60000))
                .andExpect(jsonPath("$.porcentajeDescuentoEstanciaLarga").value(10))
                .andExpect(jsonPath("$.nochesMinimasDescuento").value(7))
                .andExpect(jsonPath("$.serviciosAdicionales.length()").value(2))
                .andExpect(jsonPath("$.serviciosAdicionales[0].nombre").value("Desayuno"))
                .andExpect(jsonPath("$.serviciosAdicionales[0].costo").value(25000))
                .andExpect(jsonPath("$.serviciosAdicionales[1].nombre").value("Traslado al aeropuerto"))
                .andExpect(jsonPath("$.serviciosAdicionales[1].costo").value(80000));
    }

    @Test
    @DisplayName("CA-029, CA-030 y CA-129 Cotización recibe la limpieza, el descuento y los servicios adicionales")
    void quoteDataConTarifas() throws Exception {
        Long id = borrador();
        definir(id, TARIFAS_COMPLETAS).andExpect(status().isOk());

        // Ruta interna, sin token
        mockMvc.perform(get("/internal/listings/" + id + "/quote-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicacionId").value(id.intValue()))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.capacidad").value(4))
                .andExpect(jsonPath("$.precioPorNoche").value(250000))
                .andExpect(jsonPath("$.tarifaLimpieza").value(60000))
                .andExpect(jsonPath("$.porcentajeDescuentoEstanciaLarga").value(10))
                .andExpect(jsonPath("$.nochesMinimasDescuento").value(7))
                .andExpect(jsonPath("$.serviciosAdicionales[0].nombre").value("Desayuno"))
                .andExpect(jsonPath("$.serviciosAdicionales[0].costo").value(25000))
                .andExpect(jsonPath("$.serviciosAdicionales[1].nombre").value("Traslado al aeropuerto"));
    }

    @Test
    @DisplayName("Decisión 8: una publicación nueva entrega limpieza 0, descuento 0 y sin servicios adicionales")
    void quoteDataInicial() throws Exception {
        Long id = borrador();

        mockMvc.perform(get("/internal/listings/" + id + "/quote-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaLimpieza").value(0))
                .andExpect(jsonPath("$.porcentajeDescuentoEstanciaLarga").value(0))
                .andExpect(jsonPath("$.nochesMinimasDescuento").doesNotExist())
                .andExpect(jsonPath("$.serviciosAdicionales").isEmpty());
        verifyNoInteractions(serviciosExternos);
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(EstadoPublicacion.class)
    @DisplayName("Decisión 13: quote-data responde en cualquier estado")
    void quoteDataEnCualquierEstado(EstadoPublicacion estado) throws Exception {
        Long id = publicacion(ANFITRION, estado).getId();

        mockMvc.perform(get("/internal/listings/" + id + "/quote-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(estado.name()));
    }

    @Test
    @DisplayName("Decisión 13: quote-data de una publicación que no existe responde 404")
    void quoteDataNoExiste() throws Exception {
        mockMvc.perform(get("/internal/listings/999999/quote-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no existe"));
    }

    @Test
    @DisplayName("Decisiones 1 y 5: quitar el descuento y los servicios adicionales con 0, null y lista vacía")
    void quitaDescuento() throws Exception {
        Long id = borrador();
        definir(id, TARIFAS_COMPLETAS).andExpect(status().isOk());

        definir(id, cuerpo("0", "0", "null", "[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaLimpieza").value(0))
                .andExpect(jsonPath("$.porcentajeDescuentoEstanciaLarga").value(0))
                .andExpect(jsonPath("$.nochesMinimasDescuento").doesNotExist())
                .andExpect(jsonPath("$.serviciosAdicionales").isEmpty());
    }

    // ---------- CA-031 ----------

    @ParameterizedTest(name = "limpieza {0}, porcentaje {1}, noches {2}, costo {3}")
    @CsvSource(delimiter = '|', value = {
            "-1       | 0   | null | 25000  | " + MENSAJE_LIMPIEZA,
            "-60000   | 0   | null | 25000  | " + MENSAJE_LIMPIEZA,
            "1500.5   | 0   | null | 25000  | " + MENSAJE_LIMPIEZA,
            "null     | 0   | null | 25000  | " + MENSAJE_LIMPIEZA,
            "60000    | -1  | null | 25000  | " + MENSAJE_PORCENTAJE,
            "60000    | 91  | 7    | 25000  | " + MENSAJE_PORCENTAJE,
            "60000    | 9.5 | 7    | 25000  | " + MENSAJE_PORCENTAJE,
            "60000    | null| null | 25000  | " + MENSAJE_PORCENTAJE,
            "60000    | 10  | -7   | 25000  | " + MENSAJE_NOCHES,
            "60000    | 10  | 1    | 25000  | " + MENSAJE_NOCHES,
            "60000    | 10  | 366  | 25000  | " + MENSAJE_NOCHES,
            "60000    | 0   | null | -25000 | " + MENSAJE_COSTO,
            "60000    | 0   | null | 0      | " + MENSAJE_COSTO,
            "60000    | 0   | null | null   | " + MENSAJE_COSTO})
    @DisplayName("CA-031 un valor negativo, con decimales, fuera de rango o ausente se rechaza (400) y no cambia nada")
    void rechazaValoresInvalidos(String limpieza, String porcentaje, String noches, String costo, String mensaje)
            throws Exception {
        Long id = borrador();

        definir(id, cuerpo(limpieza, porcentaje, noches, "[{\"nombre\":\"Desayuno\",\"costo\":" + costo + "}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(1))
                .andExpect(jsonPath("$.mensajes[0]").value(mensaje));

        Publicacion guardada = publicacionRepository.findById(id).orElseThrow();
        assertEquals(0, guardada.getTarifaLimpieza().signum());
        assertEquals(0, guardada.getServiciosAdicionales().size());
    }

    @Test
    @DisplayName("CA-031 con varios valores negativos se indican todos los errores")
    void indicaTodosLosErrores() throws Exception {
        definir(borrador(), cuerpo("-1", "-1", "null", "[{\"nombre\":\"Desayuno\",\"costo\":-5}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(3))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_COSTO))
                .andExpect(jsonPath("$.mensajes[1]").value(MENSAJE_PORCENTAJE))
                .andExpect(jsonPath("$.mensajes[2]").value(MENSAJE_LIMPIEZA));
    }

    // ---------- Decisiones 5 y 6 ----------

    @Test
    @DisplayName("Decisión 5: con descuento y sin noches mínimas se rechaza (400)")
    void descuentoSinNoches() throws Exception {
        definir(borrador(), cuerpo("0", "10", "null", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Indique las noches mínimas desde las que se aplica el descuento"));
    }

    @Test
    @DisplayName("Decisión 6: servicios adicionales con nombre repetido se rechazan (400)")
    void nombreRepetido() throws Exception {
        definir(borrador(), cuerpo("0", "0", "null",
                "[{\"nombre\":\"Desayuno\",\"costo\":25000},{\"nombre\":\"desayuno\",\"costo\":20000}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("No puede haber dos servicios adicionales con el mismo nombre"));
    }

    @Test
    @DisplayName("Decisión 6: un servicio adicional sin nombre se rechaza (400)")
    void sinNombre() throws Exception {
        definir(borrador(), cuerpo("0", "0", "null", "[{\"nombre\":\" \",\"costo\":25000}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "Cada servicio adicional debe tener un nombre de máximo 60 caracteres"));
    }

    @Test
    @DisplayName("Decisión 6: más de 10 servicios adicionales se rechazan (400)")
    void masDeDiez() throws Exception {
        StringBuilder adicionales = new StringBuilder("[");
        for (int i = 1; i <= 11; i++) {
            adicionales.append(i > 1 ? "," : "").append("{\"nombre\":\"Servicio ").append(i)
                    .append("\",\"costo\":1000}");
        }
        adicionales.append("]");

        definir(borrador(), cuerpo("0", "0", "null", adicionales.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Se pueden registrar máximo 10 servicios adicionales"));
    }

    @Test
    @DisplayName("Decisión 6: sin la lista de servicios adicionales se rechaza (400)")
    void sinLista() throws Exception {
        definir(borrador(), "{\"tarifaLimpieza\":0,\"porcentajeDescuentoEstanciaLarga\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La lista de servicios adicionales es obligatoria"));
    }

    // ---------- Decisiones 1 y 2: permisos, dueño y estado ----------

    @Test
    @DisplayName("Decisión 1: un huésped no puede definir tarifas (403)")
    void rechazaHuesped() throws Exception {
        Long id = borrador();
        when(serviciosExternos.verificarAnfitrion(eq(TOKEN), anyString()))
                .thenThrow(new PermisoException(SIN_PERMISO));

        definir(id, TARIFAS_COMPLETAS)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value(SIN_PERMISO));
        verify(serviciosExternos).verificarAnfitrion(TOKEN, SIN_PERMISO);
    }

    @Test
    @DisplayName("Decisión 1: sin el encabezado Authorization se responde 401")
    void rechazaSinToken() throws Exception {
        mockMvc.perform(put("/listings/" + borrador() + "/pricing").contentType(MediaType.APPLICATION_JSON)
                        .content(TARIFAS_COMPLETAS))
                .andExpect(status().isUnauthorized());
        verify(serviciosExternos, never()).verificarAnfitrion(anyString(), anyString());
    }

    @Test
    @DisplayName("Decisión 2: la publicación de otro anfitrión responde 404")
    void publicacionAjena() throws Exception {
        Long ajena = publicacion(OTRO_ANFITRION, EstadoPublicacion.BORRADOR).getId();

        definir(ajena, TARIFAS_COMPLETAS)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no existe"));
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"PENDIENTE_REVISION", "BLOQUEADA"})
    @DisplayName("Decisión 2: en PENDIENTE_REVISION y BLOQUEADA se responde 409")
    void rechazaEstadoNoEditable(EstadoPublicacion estado) throws Exception {
        Long id = publicacion(ANFITRION, estado).getId();

        definir(id, TARIFAS_COMPLETAS)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "Solo se pueden cambiar las tarifas de una publicación en BORRADOR, ACTIVA o PAUSADA"));
    }

    @Test
    @DisplayName("Decisión 2: en una publicación ACTIVA el cambio aplica de inmediato y sigue ACTIVA")
    void activaSigueActiva() throws Exception {
        Long id = publicacion(ANFITRION, EstadoPublicacion.ACTIVA).getId();

        definir(id, TARIFAS_COMPLETAS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
        mockMvc.perform(get("/internal/listings/" + id + "/quote-data"))
                .andExpect(jsonPath("$.tarifaLimpieza").value(60000));
    }
}
