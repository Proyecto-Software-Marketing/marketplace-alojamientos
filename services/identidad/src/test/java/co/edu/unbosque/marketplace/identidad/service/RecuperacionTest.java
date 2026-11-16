package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RecuperacionRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.RestablecerRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.identidad.exception.TokenRecuperacionException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.RecuperacionContrasena;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.RecuperacionContrasenaRepository;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la HU-04 recuperación simulada de contraseña
@SpringBootTest
class RecuperacionTest {

    private static final String CORREO = "ana@correo.com";
    private static final String CLAVE_ANTERIOR = "clave12345";
    private static final String CLAVE_NUEVA = "nuevaClave2026";

    // Hora fija del reloj: 15 de noviembre de 2026, 6:00 p. m. en Bogotá
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-11-15T23:00:00Z");

    @Autowired
    private RecuperacionService recuperacionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionService sesionService;

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

    private Long idHuesped;

    // Antes de cada prueba se limpia todo y se crea una huésped
    @BeforeEach
    void prepararDatos() {
        recuperacionRepository.deleteAll();
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();
        UsuarioResponse huesped = usuarioService.registrar(
                new RegistroRequest("Ana Pérez", CORREO, CLAVE_ANTERIOR, Rol.HUESPED));
        idHuesped = huesped.getId();
        fijarReloj(AHORA);
    }

    private void fijarReloj(Instant instante) {
        when(reloj.instant()).thenReturn(instante);
        when(reloj.getZone()).thenReturn(BOGOTA);
    }

    // Pide la recuperación y devuelve el token que quedó guardado
    private String pedirToken() {
        recuperacionService.solicitar(new RecuperacionRequest(CORREO));
        return recuperacionRepository.findByUsuarioIdAndUsadoFalse(idHuesped).get(0).getToken();
    }

    private void cambiarEstado(EstadoUsuario estado) {
        Usuario usuario = usuarioRepository.findById(idHuesped).orElseThrow();
        usuario.setEstado(estado);
        usuarioRepository.save(usuario);
    }

    // ---------- CA-010 ----------

    @Test
    @DisplayName("CA-010 con mi correo registrado se genera un token y se muestra en una notificación interna")
    void generaTokenYNotifica() {
        // Dado que olvidé mi contraseña
        // Cuando pido recuperarla con mi correo
        recuperacionService.solicitar(new RecuperacionRequest(CORREO));

        // Entonces queda un token sin usar que vence en 30 minutos
        List<RecuperacionContrasena> recuperaciones = recuperacionRepository.findAll();
        assertEquals(1, recuperaciones.size());
        RecuperacionContrasena recuperacion = recuperaciones.get(0);
        assertNotNull(recuperacion.getToken());
        assertFalse(recuperacion.isUsado());
        assertEquals(LocalDateTime.of(2026, 11, 15, 18, 30), recuperacion.getFechaVencimiento());

        // Y se pide a Notificaciones mostrar el token, con el id de la solicitud como entidad
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(serviciosExternos).notificarRecuperacion(eq(idHuesped),
                eq(recuperacion.getId()), mensaje.capture());
        assertTrue(mensaje.getValue().contains(recuperacion.getToken()));
        assertTrue(mensaje.getValue().contains("30 minutos"));
        assertTrue(mensaje.getValue().length() <= 500);
    }

    @Test
    @DisplayName("CA-010 el correo se reconoce sin importar mayúsculas ni espacios")
    void correoConMayusculas() {
        recuperacionService.solicitar(new RecuperacionRequest("  Ana@Correo.COM "));

        assertEquals(1, recuperacionRepository.count());
        verify(serviciosExternos).notificarRecuperacion(any(), any(), anyString());
    }

    @Test
    @DisplayName("Decisión 3: con un correo no registrado no se genera nada ni se notifica")
    void correoNoRegistrado() {
        assertDoesNotThrow(() -> recuperacionService.solicitar(new RecuperacionRequest("nadie@correo.com")));

        assertEquals(0, recuperacionRepository.count());
        verify(serviciosExternos, never()).notificarRecuperacion(any(), any(), anyString());
    }

    @Test
    @DisplayName("Decisión 4: una cuenta restringida no recibe token")
    void restringidoNoRecibeToken() {
        cambiarEstado(EstadoUsuario.RESTRINGIDO);

        recuperacionService.solicitar(new RecuperacionRequest(CORREO));

        assertEquals(0, recuperacionRepository.count());
        verify(serviciosExternos, never()).notificarRecuperacion(any(), any(), anyString());
    }

    @Test
    @DisplayName("Decisión 4: una cuenta inactiva sí puede recuperar la contraseña")
    void inactivoRecibeToken() {
        cambiarEstado(EstadoUsuario.INACTIVO);

        recuperacionService.solicitar(new RecuperacionRequest(CORREO));

        assertEquals(1, recuperacionRepository.count());
    }

    @Test
    @DisplayName("Decisión 5: una solicitud nueva deja sin efecto el token anterior")
    void nuevaSolicitudAnulaAnterior() {
        String primero = pedirToken();
        String segundo = pedirToken();

        assertNotEquals(primero, segundo);
        assertTrue(recuperacionRepository.findByToken(primero).orElseThrow().isUsado());
        assertFalse(recuperacionRepository.findByToken(segundo).orElseThrow().isUsado());
        // Cada solicitud va con su propio id para que Notificaciones no la tome como reintento
        ArgumentCaptor<Long> ids = ArgumentCaptor.forClass(Long.class);
        verify(serviciosExternos, times(2)).notificarRecuperacion(any(), ids.capture(), anyString());
        assertNotEquals(ids.getAllValues().get(0), ids.getAllValues().get(1));
    }

    @Test
    @DisplayName("Decisión 8: si Notificaciones no responde se rechaza y no queda ningún token nuevo")
    void notificacionesCaido() {
        // Dado que ya había un token vigente
        String anterior = pedirToken();
        doThrow(new ResourceAccessException("sin conexión"))
                .when(serviciosExternos).notificarRecuperacion(anyLong(), anyLong(), anyString());

        // Cuando pido otro y Notificaciones está caído
        RecuperacionRequest datos = new RecuperacionRequest(CORREO);
        assertThrows(ServicioNoDisponibleException.class, () -> recuperacionService.solicitar(datos));

        // Entonces se revierte todo: solo existe el anterior y sigue sirviendo
        assertEquals(1, recuperacionRepository.count());
        assertFalse(recuperacionRepository.findByToken(anterior).orElseThrow().isUsado());
    }

    // ---------- CA-011 ----------

    @Test
    @DisplayName("CA-011 con un token válido defino una contraseña nueva e inicio sesión con ella")
    void restableceEIniciaSesion() {
        // Dado que tengo un token válido
        String token = pedirToken();

        // Cuando defino una contraseña nueva
        recuperacionService.restablecer(new RestablecerRequest(token, CLAVE_NUEVA));

        // Entonces puedo iniciar sesión con ella y ya no con la anterior
        SesionResponse sesion = sesionService.iniciarSesion(new LoginRequest(CORREO, CLAVE_NUEVA));
        assertNotNull(sesion.getToken());
        LoginRequest anterior = new LoginRequest(CORREO, CLAVE_ANTERIOR);
        assertThrows(AutenticacionException.class, () -> sesionService.iniciarSesion(anterior));
    }

    @Test
    @DisplayName("CA-011 después de usarlo el token deja de servir")
    void tokenDejaDeServir() {
        String token = pedirToken();
        recuperacionService.restablecer(new RestablecerRequest(token, CLAVE_NUEVA));

        assertTrue(recuperacionRepository.findByToken(token).orElseThrow().isUsado());
        RestablecerRequest otraVez = new RestablecerRequest(token, "otraClave2026");
        TokenRecuperacionException error = assertThrows(TokenRecuperacionException.class,
                () -> recuperacionService.restablecer(otraVez));
        assertEquals("Token vencido o ya usado", error.getMessage());
    }

    @Test
    @DisplayName("Decisión 7: al restablecer se cierran las sesiones abiertas")
    void cierraSesionesAbiertas() {
        SesionResponse abierta = sesionService.iniciarSesion(new LoginRequest(CORREO, CLAVE_ANTERIOR));
        String token = pedirToken();

        recuperacionService.restablecer(new RestablecerRequest(token, CLAVE_NUEVA));

        String tokenSesion = abierta.getToken();
        assertThrows(AutenticacionException.class, () -> sesionService.validarSesion(tokenSesion));
    }

    @Test
    @DisplayName("CA-011 un token usado poco antes de vencer todavía sirve")
    void tokenAntesDeVencer() {
        String token = pedirToken();
        fijarReloj(AHORA.plusSeconds(29 * 60 + 59));

        assertDoesNotThrow(() -> recuperacionService.restablecer(new RestablecerRequest(token, CLAVE_NUEVA)));
    }

    // ---------- CA-012 ----------

    @ParameterizedTest(name = "{0} minutos después")
    @ValueSource(ints = {30, 31, 120})
    @DisplayName("CA-012 un token vencido se rechaza y la contraseña no cambia")
    void tokenVencido(int minutos) {
        // Dado que pedí el token y pasaron 30 minutos o más
        String token = pedirToken();
        fijarReloj(AHORA.plusSeconds(minutos * 60L));

        // Cuando intento usarlo
        RestablecerRequest datos = new RestablecerRequest(token, CLAVE_NUEVA);
        assertThrows(TokenRecuperacionException.class, () -> recuperacionService.restablecer(datos));

        // Entonces se rechaza y sigo entrando con la contraseña anterior
        assertNotNull(sesionService.iniciarSesion(new LoginRequest(CORREO, CLAVE_ANTERIOR)).getToken());
    }

    @Test
    @DisplayName("CA-012 un token anulado por una solicitud nueva se rechaza")
    void tokenAnulado() {
        String anterior = pedirToken();
        pedirToken();

        RestablecerRequest datos = new RestablecerRequest(anterior, CLAVE_NUEVA);
        assertThrows(TokenRecuperacionException.class, () -> recuperacionService.restablecer(datos));
    }

    @Test
    @DisplayName("Decisión 6: un token que no existe se rechaza igual que uno vencido")
    void tokenInexistente() {
        RestablecerRequest datos = new RestablecerRequest("no-existe", CLAVE_NUEVA);

        TokenRecuperacionException error = assertThrows(TokenRecuperacionException.class,
                () -> recuperacionService.restablecer(datos));
        assertEquals("Token vencido o ya usado", error.getMessage());
    }

    // ---------- Decisión 10 ----------

    @Test
    @DisplayName("Decisión 10: busca el usuario por correo para el buzón de Notificaciones")
    void buscaPorCorreo() {
        assertEquals(idHuesped, recuperacionService.buscarPorCorreo(" ANA@correo.com").orElseThrow().getId());
        assertTrue(recuperacionService.buscarPorCorreo("nadie@correo.com").isEmpty());
    }
}
