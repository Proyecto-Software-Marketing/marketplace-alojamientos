package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.RecuperacionContrasenaRepository;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de las rutas de la HU-04 como las usaría el frontend
@SpringBootTest
class RecuperacionControllerTest {

    private static final String CORREO = "ana@correo.com";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RecuperacionContrasenaRepository recuperacionRepository;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Simula Notificaciones
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    @MockitoBean
    private Clock reloj;

    private MockMvc mockMvc;
    private Long idHuesped;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        recuperacionRepository.deleteAll();
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();
        UsuarioResponse huesped = usuarioService.registrar(
                new RegistroRequest("Ana Pérez", CORREO, "clave12345", Rol.HUESPED));
        idHuesped = huesped.getId();
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-15T23:00:00Z"));
        when(reloj.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    private ResultActions enviar(String ruta, String json) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String pedirToken() throws Exception {
        enviar("/autenticacion/recuperar-contrasena", "{\"correo\":\"" + CORREO + "\"}")
                .andExpect(status().isNoContent());
        return recuperacionRepository.findByUsuarioIdAndUsadoFalse(idHuesped).get(0).getToken();
    }

    // ---------- POST /autenticacion/recuperar-contrasena ----------

    @Test
    @DisplayName("CA-010 con un correo registrado responde 204 sin cuerpo y sin sesión")
    void recuperarResponde204() throws Exception {
        enviar("/autenticacion/recuperar-contrasena", "{\"correo\":\"ana@correo.com\"}")
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Decisión 3: con un correo no registrado responde igual, 204")
    void correoNoRegistradoResponde204() throws Exception {
        enviar("/autenticacion/recuperar-contrasena", "{\"correo\":\"nadie@correo.com\"}")
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Decisión 3: 400 si el correo no es válido o falta")
    void correoInvalido() throws Exception {
        enviar("/autenticacion/recuperar-contrasena", "{\"correo\":\"no-es-correo\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El correo no es válido"));
        enviar("/autenticacion/recuperar-contrasena", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El correo es obligatorio"));
    }

    @Test
    @DisplayName("Decisión 8: 503 si Notificaciones no responde")
    void notificacionesCaido() throws Exception {
        doThrow(new ResourceAccessException("sin conexión"))
                .when(serviciosExternos).notificarRecuperacion(anyLong(), anyLong(), anyString());

        enviar("/autenticacion/recuperar-contrasena", "{\"correo\":\"ana@correo.com\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("No fue posible generar la notificación de recuperación. Intente más tarde"));
    }

    // ---------- POST /autenticacion/restablecer-contrasena ----------

    @Test
    @DisplayName("CA-011 con un token válido responde 204 y puedo iniciar sesión con la contraseña nueva")
    void restablecerYEntrar() throws Exception {
        String token = pedirToken();

        enviar("/autenticacion/restablecer-contrasena",
                "{\"token\":\"" + token + "\",\"nuevaContrasena\":\"nuevaClave2026\"}")
                .andExpect(status().isNoContent());

        enviar("/autenticacion/login", "{\"correo\":\"ana@correo.com\",\"contrasena\":\"nuevaClave2026\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("CA-012 un token ya usado responde 410")
    void tokenUsadoResponde410() throws Exception {
        String token = pedirToken();
        String cuerpo = "{\"token\":\"" + token + "\",\"nuevaContrasena\":\"nuevaClave2026\"}";
        enviar("/autenticacion/restablecer-contrasena", cuerpo).andExpect(status().isNoContent());

        enviar("/autenticacion/restablecer-contrasena", cuerpo)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.mensajes[0]").value("Token vencido o ya usado"));
    }

    @Test
    @DisplayName("CA-012 un token vencido responde 410")
    void tokenVencidoResponde410() throws Exception {
        String token = pedirToken();
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-15T23:30:00Z"));

        enviar("/autenticacion/restablecer-contrasena",
                "{\"token\":\"" + token + "\",\"nuevaContrasena\":\"nuevaClave2026\"}")
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("Decisión 6: un token que no existe responde 410")
    void tokenInexistenteResponde410() throws Exception {
        enviar("/autenticacion/restablecer-contrasena", "{\"token\":\"no-existe\",\"nuevaContrasena\":\"nuevaClave2026\"}")
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("Decisión 6: 400 si la contraseña nueva tiene menos de 8 caracteres o falta el token")
    void datosInvalidos() throws Exception {
        enviar("/autenticacion/restablecer-contrasena", "{\"token\":\"abc\",\"nuevaContrasena\":\"corta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La contraseña debe tener al menos 8 caracteres"));
        enviar("/autenticacion/restablecer-contrasena", "{\"nuevaContrasena\":\"nuevaClave2026\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El token es obligatorio"));
    }

    // ---------- GET /interno/usuarios/id (Decisión 10) ----------

    @Test
    @DisplayName("Decisión 10: Notificaciones obtiene el id del usuario por su correo")
    void idPorCorreo() throws Exception {
        mockMvc.perform(get("/interno/usuarios/id").param("correo", CORREO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idHuesped));
    }

    @Test
    @DisplayName("Decisión 10: 404 si el correo no está registrado")
    void idPorCorreoNoRegistrado() throws Exception {
        mockMvc.perform(get("/interno/usuarios/id").param("correo", "nadie@correo.com"))
                .andExpect(status().isNotFound());
    }
}
