package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Pasan lso dataos del admin momentanemente como si vinieran del env
@SpringBootTest(properties = {
        "admin.correo=admin@prueba.com",
        "admin.contrasena=Admin12345"
})
class CargaInicialTest {

    @Autowired
    private SesionService sesionService;

    @Test
    @DisplayName("CA-003 el admin ya existe al arrancar y puede entrar a cosas de admin")
    void adminPrecargadoPuedeEntrar() {
        SesionResponse sesion = sesionService.iniciarSesion(new LoginRequest("admin@prueba.com", "Admin12345"));

        UsuarioResponse admin = sesionService.autorizar(sesion.getToken(), List.of(Rol.ADMINISTRADOR), null);

        assertEquals(Rol.ADMINISTRADOR, admin.getRol());
    }
}