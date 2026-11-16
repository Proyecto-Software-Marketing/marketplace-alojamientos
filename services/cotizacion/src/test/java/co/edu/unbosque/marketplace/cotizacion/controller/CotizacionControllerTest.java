package co.edu.unbosque.marketplace.cotizacion.controller;

import co.edu.unbosque.marketplace.cotizacion.client.ClienteCotizacion;
import co.edu.unbosque.marketplace.cotizacion.dto.DatosPublicacion;
import co.edu.unbosque.marketplace.cotizacion.dto.ServicioAdicionalCotizado;
import co.edu.unbosque.marketplace.cotizacion.exception.AutenticacionException;
import co.edu.unbosque.marketplace.cotizacion.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.cotizacion.exception.PermisoException;
import co.edu.unbosque.marketplace.cotizacion.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.cotizacion.model.ConfiguracionTarifa;
import co.edu.unbosque.marketplace.cotizacion.repository.ConfiguracionTarifaRepository;
import co.edu.unbosque.marketplace.cotizacion.repository.CotizacionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-22, como las harían el frontend (POST) y Reservas (GET).
// Identidad y Alojamientos se simulan con ClienteCotizacion
@SpringBootTest
class CotizacionControllerTest {

    private static final String TOKEN = "Bearer token-huesped";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    // 7 noches desde el 1 de diciembre, 3 huéspedes, con desayuno
    private static final String COTIZACION = "{\"publicacionId\":1,\"fechaLlegada\":\"2026-12-01\","
            + "\"fechaSalida\":\"2026-12-08\",\"cantidadHuespedes\":3,\"serviciosAdicionales\":[\"Desayuno\"]}";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private ConfiguracionTarifaRepository configuracionTarifaRepository;

    @MockitoBean
    private ClienteCotizacion clienteCotizacion;

    @MockitoBean
    private Clock reloj;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        cotizacionRepository.deleteAll();
        configuracionTarifaRepository.deleteAll();
        configuracionTarifaRepository.save(new ConfiguracionTarifa(new BigDecimal("15"), 1L));
        when(reloj.getZone()).thenReturn(BOGOTA);
        when(reloj.instant()).thenReturn(HOY.atTime(10, 0).atZone(BOGOTA).toInstant());
        when(clienteCotizacion.verificarHuesped(TOKEN)).thenReturn(2L);
        when(clienteCotizacion.consultarDatosPublicacion(1L)).thenReturn(publicacion());
    }

    private static DatosPublicacion publicacion() {
        DatosPublicacion datos = new DatosPublicacion();
        datos.setPublicacionId(1L);
        datos.setEstado("ACTIVA");
        datos.setCapacidad(4);
        datos.setPrecioPorNoche(new BigDecimal("250000"));
        datos.setTarifaLimpieza(new BigDecimal("60000"));
        datos.setPorcentajeDescuentoEstanciaLarga(10);
        datos.setNochesMinimasDescuento(7);
        datos.setServiciosAdicionales(List.of(new ServicioAdicionalCotizado("Desayuno", new BigDecimal("25000"))));
        return datos;
    }

    private ResultActions cotizar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/quotes").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---------- CA-060 a CA-062 ----------

    @Test
    @DisplayName("CA-060, CA-061 y CA-062 el huésped recibe la cotización desglosada (201)")
    void cotizacionDesglosada() throws Exception {
        cotizar(COTIZACION)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.publicacionId").value(1))
                .andExpect(jsonPath("$.fechaLlegada").value("2026-12-01"))
                .andExpect(jsonPath("$.fechaSalida").value("2026-12-08"))
                .andExpect(jsonPath("$.cantidadHuespedes").value(3))
                .andExpect(jsonPath("$.noches").value(7))
                .andExpect(jsonPath("$.precioPorNoche").value(250000))
                .andExpect(jsonPath("$.precioBase").value(1750000))
                .andExpect(jsonPath("$.tarifaLimpieza").value(60000))
                .andExpect(jsonPath("$.serviciosAdicionales[0].nombre").value("Desayuno"))
                .andExpect(jsonPath("$.serviciosAdicionales[0].costo").value(25000))
                .andExpect(jsonPath("$.porcentajeDescuento").value(10))
                .andExpect(jsonPath("$.descuento").value(175000))
                .andExpect(jsonPath("$.porcentajeTarifaServicio").value(15))
                .andExpect(jsonPath("$.tarifaServicio").value(249000))
                .andExpect(jsonPath("$.total").value(1909000));
        assertEquals(1, cotizacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 1: Reservas consulta la cotización por su id, sin token, con los datos que usa")
    void reservasConsultaLaCotizacion() throws Exception {
        MvcResult creada = cotizar(COTIZACION).andExpect(status().isCreated()).andReturn();
        String id = JsonPath.read(creada.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/quotes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.publicacionId").value(1))
                .andExpect(jsonPath("$.fechaLlegada").value("2026-12-01"))
                .andExpect(jsonPath("$.fechaSalida").value("2026-12-08"))
                .andExpect(jsonPath("$.cantidadHuespedes").value(3))
                .andExpect(jsonPath("$.total").value(1909000));
    }

    @Test
    @DisplayName("Decisión 1: una cotización que no existe responde 404")
    void consultarNoExiste() throws Exception {
        mockMvc.perform(get("/quotes/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La cotización no existe"));
    }

    @Test
    @DisplayName("CA-061 sin servicios adicionales la lista se puede omitir")
    void sinServiciosAdicionales() throws Exception {
        cotizar("{\"publicacionId\":1,\"fechaLlegada\":\"2026-12-01\",\"fechaSalida\":\"2026-12-03\","
                + "\"cantidadHuespedes\":1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviciosAdicionales").isEmpty())
                // 500.000 + 60.000 = 560.000; 15 % = 84.000
                .andExpect(jsonPath("$.total").value(644000));
    }

    // ---------- CA-063 ----------

    @Test
    @DisplayName("CA-063 más huéspedes que la capacidad no genera la cotización (422)")
    void superaCapacidad() throws Exception {
        cotizar(COTIZACION.replace("\"cantidadHuespedes\":3", "\"cantidadHuespedes\":5"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación admite máximo 4 huéspedes"));
        assertEquals(0, cotizacionRepository.count());
    }

    // ---------- Decisiones 3 a 7: errores ----------

    @Test
    @DisplayName("Decisión 3: sin datos se indica cada campo obligatorio (400)")
    void cuerpoVacio() throws Exception {
        cotizar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(4));
        verify(clienteCotizacion, never()).verificarHuesped(anyString());
    }

    @Test
    @DisplayName("Decisión 3: cero huéspedes se rechaza (400)")
    void ceroHuespedes() throws Exception {
        cotizar(COTIZACION.replace("\"cantidadHuespedes\":3", "\"cantidadHuespedes\":0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe haber al menos un huésped"));
    }

    @Test
    @DisplayName("Decisión 3: una fecha con formato inválido se rechaza (400)")
    void fechaInvalida() throws Exception {
        cotizar(COTIZACION.replace("2026-12-01", "01/12/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }

    @Test
    @DisplayName("Decisión 4: una salida antes de la llegada se rechaza (400)")
    void salidaAntesDeLlegada() throws Exception {
        cotizar(COTIZACION.replace("2026-12-08", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La fecha de salida debe ser posterior a la de llegada"));
    }

    @Test
    @DisplayName("Decisión 7: un servicio adicional que la publicación no ofrece se rechaza (400)")
    void servicioNoOfrecido() throws Exception {
        cotizar(COTIZACION.replace("\"Desayuno\"", "\"Spa\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no ofrece el servicio adicional: Spa"));
    }

    @Test
    @DisplayName("Decisión 5: una publicación que no existe responde 404 y una que no está ACTIVA 409")
    void publicacionNoExisteONoActiva() throws Exception {
        when(clienteCotizacion.consultarDatosPublicacion(1L)).thenThrow(new NoEncontradoException("La publicación no existe"));
        cotizar(COTIZACION)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no existe"));

        DatosPublicacion pausada = publicacion();
        pausada.setEstado("PAUSADA");
        doReturn(pausada).when(clienteCotizacion).consultarDatosPublicacion(1L);
        cotizar(COTIZACION)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("La publicación no está disponible para cotizar"));
    }

    @Test
    @DisplayName("Decisión 5: si Alojamientos no responde se responde 503")
    void alojamientosCaido() throws Exception {
        when(clienteCotizacion.consultarDatosPublicacion(anyLong())).thenThrow(
                new ServicioNoDisponibleException("No fue posible consultar la publicación. Intente más tarde"));

        cotizar(COTIZACION)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes[0]").value("No fue posible consultar la publicación. Intente más tarde"));
    }

    // ---------- Decisión 1: sesión y rol ----------

    @Test
    @DisplayName("Decisión 1: solo un huésped puede cotizar (403)")
    void noEsHuesped() throws Exception {
        when(clienteCotizacion.verificarHuesped(TOKEN))
                .thenThrow(new PermisoException("Solo un huésped puede pedir una cotización"));

        cotizar(COTIZACION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo un huésped puede pedir una cotización"));
        verify(clienteCotizacion, never()).consultarDatosPublicacion(anyLong());
    }

    @Test
    @DisplayName("Decisión 1: sin token o con el token inválido se responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(post("/quotes").contentType(MediaType.APPLICATION_JSON).content(COTIZACION))
                .andExpect(status().isUnauthorized());

        when(clienteCotizacion.verificarHuesped(TOKEN)).thenThrow(new AutenticacionException("Debe iniciar sesión"));
        cotizar(COTIZACION)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
        assertEquals(0, cotizacionRepository.count());
    }
}
