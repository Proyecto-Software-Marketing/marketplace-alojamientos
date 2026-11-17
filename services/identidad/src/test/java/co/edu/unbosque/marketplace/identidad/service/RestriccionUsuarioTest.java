package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.exception.CuentaException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.model.Sesion;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "servicios.auditoria.url=",
        "servicios.notificaciones.url=",
        "servicios.alojamientos.url=",
        "servicios.reservas.url="
})
class RestriccionUsuarioTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionService sesionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SesionRepository sesionRepository;

    private Long usuarioId;

    @BeforeEach
    void prepararDatos() {
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();

        UsuarioResponse usuario = usuarioService.registrar(
                new RegistroRequest(
                        "Carlos Perez",
                        "carlos@correo.com",
                        "clave12345",
                        Rol.HUESPED));

        usuarioId = usuario.getId();
    }

    @Test
    @DisplayName("HU-35: restringir usuario cambia su estado")
    void restringirUsuario() {
        UsuarioResponse respuesta =
                usuarioService.restringirUsuario(usuarioId, 999L);

        assertEquals(EstadoUsuario.RESTRINGIDO, respuesta.getEstado());
    }

    @Test
    @DisplayName("HU-35: restringir usuario cierra sus sesiones")
    void restringirCierraSesiones() {
        String token = sesionService.iniciarSesion(
                new LoginRequest("carlos@correo.com", "clave12345"))
                .getToken();

        usuarioService.restringirUsuario(usuarioId, 999L);

        assertTrue(sesionRepository.findByTokenAndActivaTrue(token).isEmpty());

        assertThrows(
                AutenticacionException.class,
                () -> sesionService.validarSesion(token));
    }

    @Test
    @DisplayName("HU-35: no permite restringir administradores")
    void noPermiteRestringirAdministrador() {
        Usuario administrador = new Usuario(
                "Administrador",
                "admin@correo.com",
                "clave",
                Rol.ADMINISTRADOR);

        usuarioRepository.save(administrador);

        assertThrows(
                CuentaException.class,
                () -> usuarioService.restringirUsuario(administrador.getId(), 999L));
    }

    @Test
    @DisplayName("HU-35: no permite restringir dos veces")
    void noPermiteRestringirDosVeces() {
        usuarioService.restringirUsuario(usuarioId, 999L);

        assertThrows(
                CuentaException.class,
                () -> usuarioService.restringirUsuario(usuarioId, 999L));
    }

    @Test
    @DisplayName("HU-35: administrador puede reactivar usuario restringido")
    void reactivarUsuario() {
        usuarioService.restringirUsuario(usuarioId, 999L);

        UsuarioResponse respuesta =
                usuarioService.reactivarUsuario(usuarioId, 999L);

        assertEquals(EstadoUsuario.ACTIVO, respuesta.getEstado());
    }

    @Test
    @DisplayName("HU-35: no permite reactivar una cuenta que no está restringida")
    void noPermiteReactivarCuentaActiva() {
        assertThrows(
                CuentaException.class,
                () -> usuarioService.reactivarUsuario(usuarioId, 999L));
    }

    @Test
    @DisplayName("HU-35: usuario restringido no puede reactivar su cuenta")
    void usuarioNoPuedeReactivarCuentaRestringida() {
        usuarioService.restringirUsuario(usuarioId, 999L);

        assertThrows(
                AutenticacionException.class,
                () -> sesionService.reactivarCuenta(
                        new LoginRequest("carlos@correo.com", "clave12345")));
    }
}