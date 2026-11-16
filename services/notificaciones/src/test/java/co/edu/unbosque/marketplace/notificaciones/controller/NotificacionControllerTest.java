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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // ---------- HU-04 (CA-010, Decisión 10): GET /notifications/password-recovery ----------

    @Test
    @DisplayName("CA-010 y Decisión 10: sin sesión muestra solo la última notificación de recuperación del correo")
    void buzonMuestraUltimaRecuperacion() throws Exception {
        // Dado que el huésped pidió recuperar la contraseña dos veces y tiene otras notificaciones
        when(serviciosExternos.buscarUsuarioPorCorreo("ana@correo.com")).thenReturn(Optional.of(HUESPED));
        notificacion(HUESPED, TipoEvento.RECUPERACION_CONTRASENA, 1L, 2);
        notificacion(HUESPED, TipoEvento.RECUPERACION_CONTRASENA, 2L, 1);
        notificacion(HUESPED, TipoEvento.RESERVA_CONFIRMADA, 300L, 0);
        notificacion(OTRO_USUARIO, TipoEvento.RECUPERACION_CONTRASENA, 3L, 0);

        // Cuando abre el buzón simulado con su correo (sin token, con mayúsculas y espacios)
        mockMvc.perform(get("/notifications/password-recovery").param("correo", "  Ana@Correo.com "))
                // Entonces solo ve la notificación de recuperación más reciente
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].tipoEvento").value("RECUPERACION_CONTRASENA"))
                .andExpect(jsonPath("$.items[0].entidadTipo").value("USUARIO"))
                .andExpect(jsonPath("$.items[0].entidadId").value(2))
                .andExpect(jsonPath("$.noLeidas").value(1));
        verify(serviciosExternos, never()).verificarSesion(anyString());
    }

    @Test
    @DisplayName("Decisión 10: un correo no registrado responde 200 con la lista vacía")
    void buzonCorreoNoRegistrado() throws Exception {
        when(serviciosExternos.buscarUsuarioPorCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/notifications/password-recovery").param("correo", "nadie@correo.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.noLeidas").value(0));
    }

    @Test
    @DisplayName("Decisión 10: un usuario sin solicitudes de recuperación ve la lista vacía")
    void buzonSinRecuperaciones() throws Exception {
        when(serviciosExternos.buscarUsuarioPorCorreo("ana@correo.com")).thenReturn(Optional.of(HUESPED));
        notificacion(HUESPED, TipoEvento.RESERVA_CONFIRMADA, 300L, 0);

        mockMvc.perform(get("/notifications/password-recovery").param("correo", "ana@correo.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("Decisión 10: 400 si no se envía el correo o viene vacío")
    void buzonSinCorreo() throws Exception {
        mockMvc.perform(get("/notifications/password-recovery"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El correo es obligatorio"));
        mockMvc.perform(get("/notifications/password-recovery").param("correo", "  "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Decisión 10: 503 si Identidad no responde al abrir el buzón")
    void buzonIdentidadCaida() throws Exception {
        when(serviciosExternos.buscarUsuarioPorCorreo(anyString()))
                .thenThrow(new ServicioNoDisponibleException("No fue posible consultar el usuario. Intente más tarde"));

        mockMvc.perform(get("/notifications/password-recovery").param("correo", "ana@correo.com"))
                .andExpect(status().isServiceUnavailable());
    }
}
