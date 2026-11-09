package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.exception.PermisoException;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pruebas de  control de acceso por rol 
@SpringBootTest
class AutorizacionTest {

    @Autowired
    private SesionService sesionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String tokenHuesped;
    private String tokenAnfitrion;
    private Long idAnfitrion;

    @BeforeEach
    void prepararDatos() {
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.registrar(new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED));
        UsuarioResponse anfitrion = usuarioService.registrar(
                new RegistroRequest("Luis Gómez", "luis@correo.com", "clave12345", Rol.ANFITRION));
        idAnfitrion = anfitrion.getId();

        tokenHuesped = sesionService.iniciarSesion(new LoginRequest("ana@correo.com", "clave12345")).getToken();
        tokenAnfitrion = sesionService.iniciarSesion(new LoginRequest("luis@correo.com", "clave12345")).getToken();
    }

    @Test
    @DisplayName("CA-007 el huésped no puede hacer cosas de anfitrión o admin")
    void huespedNoTienePermiso() {
        List<Rol> soloAnfitrionOAdmin = List.of(Rol.ANFITRION, Rol.ADMINISTRADOR);

        assertThrows(PermisoException.class,
                () -> sesionService.autorizar(tokenHuesped, soloAnfitrionOAdmin, null));
    }

    @Test
    @DisplayName("CA-007 el anfitrión sí puede hacer cosas de anfitrión")
    void anfitrionTienePermiso() {
        UsuarioResponse usuario = sesionService.autorizar(tokenAnfitrion, List.of(Rol.ANFITRION), null);

        assertEquals(Rol.ANFITRION, usuario.getRol());
    }

    @Test
    @DisplayName("CA-008 sin sesión no se puede entrar")
    void sinSesionNoEntra() {
        assertThrows(AutenticacionException.class,
                () -> sesionService.autorizar("token-inventado", List.of(Rol.HUESPED), null));
    }

    @Test
    @DisplayName("CA-009 el anfitrión no puede tocar algo de otro anfitrión")
    void anfitrionNoEsElDueno() {
        Long idOtroAnfitrion = idAnfitrion + 100;

        assertThrows(PermisoException.class,
                () -> sesionService.autorizar(tokenAnfitrion, List.of(Rol.ANFITRION), idOtroAnfitrion));
    }

    @Test
    @DisplayName("CA-009 el anfitrión sí puede tocar lo suyo")
    void anfitrionEsElDueno() {
        UsuarioResponse usuario = sesionService.autorizar(tokenAnfitrion, List.of(Rol.ANFITRION), idAnfitrion);

        assertEquals(idAnfitrion, usuario.getId());
    }
}