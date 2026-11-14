package co.edu.unbosque.marketplace.disponibilidad.controller;

import co.edu.unbosque.marketplace.disponibilidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.disponibilidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.disponibilidad.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.disponibilidad.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API del calendario (HU-17), como las haría el frontend
@SpringBootTest
class CalendarioControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final String RUTA = "/listings/15/calendar";
    private static final Long PUBLICACION = 15L;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PeriodoDisponibleRepository periodoRepository;

    @Autowired
    private BloqueoReservaRepository bloqueoRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        bloqueoRepository.deleteAll();
        periodoRepository.deleteAll();
        // Por defecto la publicación está activa
        when(serviciosExternos.consultarEstadoPublicacion(anyLong())).thenReturn("ACTIVA");
    }

    private LocalDate dia(int diasDesdeHoy) {
        return LocalDate.now().plusDays(diasDesdeHoy);
    }

    private void periodo(LocalDate inicio, LocalDate fin) {
        periodoRepository.save(new PeriodoDisponible(PUBLICACION, inicio, fin));
    }

    private void reserva(LocalDate llegada, LocalDate salida) {
        bloqueoRepository.save(new BloqueoReserva(PUBLICACION, 500L, llegada, salida));
    }

    // ---------- CA-048 ----------

    @Test
    @DisplayName("CA-048 GET del calendario responde 200 con los tramos diferenciados")
    void calendarioConTramos() throws Exception {
        periodo(dia(1), dia(20));
        reserva(dia(5), dia(8));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPublicacion").value(15))
                .andExpect(jsonPath("$.periodos.length()").value(3))
                .andExpect(jsonPath("$.periodos[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.periodos[1].estado").value("RESERVADO"))
                .andExpect(jsonPath("$.periodos[1].fechaInicio").value(dia(5).toString()))
                .andExpect(jsonPath("$.periodos[1].fechaFin").value(dia(8).toString()))
                .andExpect(jsonPath("$.periodos[2].estado").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("CA-048 una publicación bloqueada se muestra con estado BLOQUEADO")
    void calendarioBloqueado() throws Exception {
        when(serviciosExternos.consultarEstadoPublicacion(anyLong())).thenReturn("BLOQUEADA");
        periodo(dia(1), dia(20));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodos.length()").value(1))
                .andExpect(jsonPath("$.periodos[0].estado").value("BLOQUEADO"));
    }

    @Test
    @DisplayName("Una publicación sin periodos responde 200 con el calendario vacío")
    void calendarioVacio() throws Exception {
        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodos").isEmpty());
    }

    // ---------- CA-049 ----------

    @Test
    @DisplayName("CA-049 después de confirmarse una reserva el calendario la muestra como reservada")
    void reservaApareceEnElCalendario() throws Exception {
        periodo(dia(1), dia(10));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(jsonPath("$.periodos.length()").value(1))
                .andExpect(jsonPath("$.periodos[0].estado").value("DISPONIBLE"));

        // Dado que se confirma una reserva (la registrará la HU-18)
        reserva(dia(3), dia(6));

        // Cuando vuelvo a consultar, entonces el periodo aparece como reservado
        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(jsonPath("$.periodos.length()").value(3))
                .andExpect(jsonPath("$.periodos[1].estado").value("RESERVADO"));
    }

    // ---------- Permisos y errores ----------

    @Test
    @DisplayName("Sin encabezado Authorization responde 401")
    void sinToken() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con un token inválido responde 401")
    void tokenInvalido() throws Exception {
        doThrow(new AutenticacionException("Debe iniciar sesión"))
                .when(serviciosExternos).verificarSesion(anyString());

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("Si la publicación no existe responde 404")
    void publicacionNoExiste() throws Exception {
        when(serviciosExternos.consultarEstadoPublicacion(anyLong()))
                .thenThrow(new NoEncontradoException("La publicación no existe"));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Si Alojamientos no responde se devuelve 503")
    void alojamientosCaido() throws Exception {
        when(serviciosExternos.consultarEstadoPublicacion(anyLong()))
                .thenThrow(new ServicioNoDisponibleException("No fue posible consultar la publicación. Intente más tarde"));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isServiceUnavailable());
    }
}