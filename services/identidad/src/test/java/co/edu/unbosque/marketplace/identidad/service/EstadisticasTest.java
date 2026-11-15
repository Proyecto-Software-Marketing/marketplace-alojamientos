package co.edu.unbosque.marketplace.identidad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import co.edu.unbosque.marketplace.identidad.controller.InternoController;
import co.edu.unbosque.marketplace.identidad.dto.EstadisticasUsuariosResponse;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;

// Pruebas del conteo de usuarios para el dashboard
@SpringBootTest
class EstadisticasTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private InternoController internoController;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void prepararDatos() {
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioService.registrar(new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED));
        usuarioService.registrar(new RegistroRequest("Luis Gómez", "luis@correo.com", "clave12345", Rol.HUESPED));
        usuarioService.registrar(new RegistroRequest("Marta Ruiz", "marta@correo.com", "clave12345", Rol.ANFITRION));
    }

    @Test
    @DisplayName("CA-108 cuenta los usuarios en total y por rol")
    void cuentaLosUsuarios() {
        EstadisticasUsuariosResponse estadisticas = internoController.estadisticas();

        assertEquals(3, estadisticas.getTotal());
        assertEquals(2, estadisticas.getHuespedes());
        assertEquals(1, estadisticas.getAnfitriones());
    }

    @Test
    @DisplayName("CA-109 si se registra alguien nuevo, el conteo se actualiza")
    void elConteoSeActualiza() {
        usuarioService.registrar(new RegistroRequest("Pedro Díaz", "pedro@correo.com", "clave12345", Rol.ANFITRION));

        EstadisticasUsuariosResponse estadisticas = usuarioService.estadisticas();

        assertEquals(4, estadisticas.getTotal());
        assertEquals(2, estadisticas.getAnfitriones());
    }
}