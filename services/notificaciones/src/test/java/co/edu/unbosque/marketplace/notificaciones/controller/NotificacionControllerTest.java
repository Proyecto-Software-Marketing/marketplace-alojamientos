package co.edu.unbosque.marketplace.notificaciones.controller;

import co.edu.unbosque.marketplace.notificaciones.client.ServiciosExternos;
import co.edu.unbosque.marketplace.notificaciones.exception.AutenticacionException;
import co.edu.unbosque.marketplace.notificaciones.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.model.TipoEvento;
import co.edu.unbosque.marketplace.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la API REST de la HU-31, como las haría el frontend
@SpringBootTest
class NotificacionControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";
    private static final Long HUESPED = 21L;
    private static final Long OTRO_USUARIO = 99L;
    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 15, 10, 0);

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // Identidad simulada: el token es del huésped 21
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        notificacionRepository.deleteAll();
        when(serviciosExternos.verificarSesion(TOKEN)).thenReturn(HUESPED);
    }

    private Notificacion notificacion(Long idUsuario, TipoEvento tipo, Long idEntidad, int horasAtras) {
        return notificacionRepository.save(new Notificacion(idUsuario, tipo, idEntidad, tipo.getMensajePorDefecto(),
                HOY.minusHours(horasAtras)));
    }

    // ---------- CA-085 y CA-087: GET /notifications ----------

    @Test
    @DisplayName("CA-085 y CA-087 devuelve solo mis notificaciones, la más reciente primero, con el número de no leídas")
    void listaMisNotificaciones() throws Exception {
        Notificacion antigua = notificacion(HUESPED, TipoEvento.RESERVA_CONFIRMADA, 300L, 5);
        notificacion(HUESPED, TipoEvento.RESERVA_CANCELADA, 301L, 1);
        notificacion(OTRO_USUARIO, TipoEvento.PUBLICACION_APROBADA, 15L, 0);
        antigua.marcarLeida();
        notificacionRepository.save(antigua);

        mockMvc.perform(get("/notifications").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].entidadId").value(301))
                .andExpect(jsonPath("$.items[0].leida").value(false))
                .andExpect(jsonPath("$.items[0].tipoEvento").value("RESERVA_CANCELADA"))
                .andExpect(jsonPath("$.items[0].creadaEn").exists())
                .andExpect(jsonPath("$.items[1].entidadId").value(300))
                .andExpect(jsonPath("$.items[1].leida").value(true))
                .andExpect(jsonPath("$.noLeidas").value(1));
    }

    @Test
    @DisplayName("CA-085 sin notificaciones responde 200 con la lista vacía")
    void listaVacia() throws Exception {
        mockMvc.perform(get("/notifications").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.noLeidas").value(0));
    }

    // ---------- CA-086 y CA-087: PATCH /notifications/{id}/read ----------

    @Test
    @DisplayName("CA-086 al marcar como leída responde 200 con leida = true")
    void marcaLeida() throws Exception {
        Notificacion pendiente = notificacion(HUESPED, TipoEvento.RESERVA_CONFIRMADA, 300L, 1);

        mockMvc.perform(patch("/notifications/{id}/read", pendiente.getId()).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendiente.getId()))
                .andExpect(jsonPath("$.leida").value(true));

        assertTrue(notificacionRepository.findById(pendiente.getId()).orElseThrow().isLeida());
    }

    @Test
    @DisplayName("CA-087 / Decisión 2: marcar la notificación de otro usuario responde 404")
    void noMarcaLaDeOtro() throws Exception {
        Notificacion ajena = notificacion(OTRO_USUARIO, TipoEvento.PUBLICACION_APROBADA, 15L, 1);

        mockMvc.perform(patch("/notifications/{id}/read", ajena.getId()).header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La notificación no existe"));

        assertFalse(notificacionRepository.findById(ajena.getId()).orElseThrow().isLeida());
    }

    @Test
    @DisplayName("400 si el id de la notificación no es un número")
    void idInvalido() throws Exception {
        mockMvc.perform(patch("/notifications/abc/read").header("Authorization", TOKEN))
                .andExpect(status().isBadRequest());
    }

    // ---------- Sesión ----------

    @Test
    @DisplayName("401 si no se envía el token")
    void sinToken() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("401 si el token no es válido")
    void tokenInvalido() throws Exception {
        when(serviciosExternos.verificarSesion(anyString())).thenThrow(new AutenticacionException("Debe iniciar sesión"));

        mockMvc.perform(patch("/notifications/1/read").header("Authorization", "Bearer vencido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("503 si Identidad no responde")
    void identidadCaida() throws Exception {
        when(serviciosExternos.verificarSesion(TOKEN))
                .thenThrow(new ServicioNoDisponibleException("No fue posible validar la sesión. Intente más tarde"));

        mockMvc.perform(get("/notifications").header("Authorization", TOKEN))
                .andExpect(status().isServiceUnavailable());
    }
}
