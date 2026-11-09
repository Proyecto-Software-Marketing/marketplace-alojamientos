package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.RegistroException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pruebas de la Historia de usuario para el registro de usuarios 
// cada preuba es para un criterio de aceptación

@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpiarBaseDeDatos() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("CA-001: con datos válidos se crea la cuenta con el rol elegido")
    void registraHuespedConDatosValidos() {
        RegistroRequest datos = new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED);

        UsuarioResponse respuesta = usuarioService.registrar(datos);

        assertNotNull(respuesta.getId());
        assertEquals("ana@correo.com", respuesta.getCorreo());
        assertEquals(Rol.HUESPED, respuesta.getRol());
        assertEquals(EstadoUsuario.ACTIVO, respuesta.getEstado());
    }

    @Test
    @DisplayName("CA-001: la contraseña se guarda cifrada, no la original")
    void guardaLaContrasenaCifrada() {
        RegistroRequest datos = new RegistroRequest("Luis Gómez", "luis@correo.com", "clave12345", Rol.ANFITRION);

        UsuarioResponse respuesta = usuarioService.registrar(datos);

        Usuario guardado = usuarioRepository.findById(respuesta.getId()).orElseThrow();
        assertNotEquals("clave12345", guardado.getContrasena());
    }

    @Test
    @DisplayName("CA-002: no se puede registrar un correo que ya está en uso")
    void rechazaCorreoRepetido() {
        usuarioService.registrar(new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED));
        RegistroRequest repetido = new RegistroRequest("Anita Perez", "ANA@correo.com", "clave76", Rol.ANFITRION);

        RegistroException error = assertThrows(RegistroException.class, () -> usuarioService.registrar(repetido));

        assertEquals("El correo ya está en uso", error.getMessage());
        assertEquals(1, usuarioRepository.count());
    }

    @Test
    @DisplayName("CA-003: no se puede elegir el rol Administrador")
    void rechazaRolAdministrador() {
        RegistroRequest datos = new RegistroRequest("Pedro Ruiz", "pedro@correo.com", "clave12345", Rol.ADMINISTRADOR);

        RegistroException error = assertThrows(RegistroException.class, () -> usuarioService.registrar(datos));

        assertEquals("No es posible registrarse como Administrador", error.getMessage());
        assertEquals(0, usuarioRepository.count());
    }

    @Test
    @DisplayName("CA-127: la cuenta queda con un único rol")
    void cuentaQuedaConUnSoloRol() {
        UsuarioResponse respuesta = usuarioService.registrar(
                new RegistroRequest("María Díaz", "maria@correo.com", "clave12345", Rol.ANFITRION));

        Usuario guardado = usuarioRepository.findById(respuesta.getId()).orElseThrow();
        assertEquals(Rol.ANFITRION, guardado.getRol());
    }
}