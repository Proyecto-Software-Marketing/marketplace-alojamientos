package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.PerfilRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.RegistroException;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pruebas de la HU-05 para consultar y actualizar perfil
@SpringBootTest
class PerfilTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Revisa las validaciones del formulario 
    @Autowired
    private Validator validador;

    private Long idAna;

    // Antes de cada prueba se limpia todo y se registra
    @BeforeEach
    void prepararDatos() {
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();
        UsuarioResponse ana = usuarioService.registrar(
                new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED));
        idAna = ana.getId();
    }

    @Test
    @DisplayName("CA-013 ve sus datos básicos")
    void consultaSusDatos() {
        UsuarioResponse perfil = usuarioService.consultarPerfil(idAna);

        assertEquals("Ana Pérez", perfil.getNombre());
        assertEquals("ana@correo.com", perfil.getCorreo());
    }

    @Test
    @DisplayName("CA-014 los cambios se guardan y se ven actualizados")
    void actualizaSusDatos() {
        usuarioService.actualizarPerfil(idAna, new PerfilRequest("Ana María Pérez", "anamaria@correo.com"));

        UsuarioResponse perfil = usuarioService.consultarPerfil(idAna);
        assertEquals("Ana María Pérez", perfil.getNombre());
        assertEquals("anamaria@correo.com", perfil.getCorreo());
    }

    @Test
    @DisplayName("CA-014 no puede poner un correo que ya tiene otra cuenta")
    void noPuedeUsarCorreoDeOtro() {
        usuarioService.registrar(new RegistroRequest("Luis Gómez", "luis@correo.com", "clave12345", Rol.ANFITRION));
        PerfilRequest datos = new PerfilRequest("Ana Pérez", "luis@correo.com");

        assertThrows(RegistroException.class, () -> usuarioService.actualizarPerfil(idAna, datos));
    }

    @Test
    @DisplayName("CA-015 nombre vacío no pasa")
    void nombreVacioNoPasa() {
        PerfilRequest datos = new PerfilRequest("", "ana@correo.com");

        assertFalse(validador.validate(datos).isEmpty());
    }

    @Test
    @DisplayName("CA-015 correo con formato inválido no pasa")
    void correoInvalidoNoPasa() {
        PerfilRequest datos = new PerfilRequest("Ana Pérez", "esto-no-es-un-correo");

        assertFalse(validador.validate(datos).isEmpty());
    }
}