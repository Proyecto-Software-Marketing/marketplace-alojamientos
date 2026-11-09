package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pruebas de la Historia de usuario para el Inicio y cierre de sesión
// cada preuba es para un criterio de aceptación

@SpringBootTest
class SesionServiceTest {

    @Autowired
    private SesionService sesionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    
    @BeforeEach
    void prepararDatos() {
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioService.registrar(new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED));
    }

    @Test
    @DisplayName("CA-004: con credenciales correctas entra y recibe su rol")
    void iniciaSesionConCredencialesCorrectas() {
        SesionResponse sesion = sesionService.iniciarSesion(new LoginRequest("ana@correo.com", "clave12345"));

        assertNotNull(sesion.getToken());
        assertEquals("ana@correo.com", sesion.getUsuario().getCorreo());
        assertEquals(Rol.HUESPED, sesion.getUsuario().getRol());
    }

    @Test
    @DisplayName("CA-005: con contraseña incorrecta no entra y el mensaje es general")
    void rechazaContrasenaIncorrecta() {
        LoginRequest datos = new LoginRequest("ana@correo.com", "otraClave99");

        AutenticacionException error = assertThrows(AutenticacionException.class,
                () -> sesionService.iniciarSesion(datos));

        assertEquals("Correo o contraseña incorrectos", error.getMessage());
    }

    @Test
    @DisplayName("CA-005: con un correo que no existe el mensaje es el mismo")
    void rechazaCorreoInexistenteConElMismoMensaje() {
        LoginRequest datos = new LoginRequest("noexiste@correo.com", "clave12345");

        AutenticacionException error = assertThrows(AutenticacionException.class,
                () -> sesionService.iniciarSesion(datos));

        assertEquals("Correo o contraseña incorrectos", error.getMessage());
    }

    @Test
    @DisplayName("CA-006: con la sesión activa se puede consultar el usuario")
    void sesionActivaPermiteAcceso() {
        SesionResponse sesion = sesionService.iniciarSesion(new LoginRequest("ana@correo.com", "clave12345"));

        UsuarioResponse usuario = sesionService.validarSesion(sesion.getToken());

        assertEquals("ana@correo.com", usuario.getCorreo());
    }

    @Test
    @DisplayName("CA-006: después de cerrar sesión ya no se puede acceder")
    void despuesDeCerrarSesionNoHayAcceso() {
        SesionResponse sesion = sesionService.iniciarSesion(new LoginRequest("ana@correo.com", "clave12345"));

        sesionService.cerrarSesion(sesion.getToken());

        assertThrows(AutenticacionException.class, () -> sesionService.validarSesion(sesion.getToken()));
    }
}