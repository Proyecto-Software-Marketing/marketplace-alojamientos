package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.exception.CuentaException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la HU-06 desactivar y reactivar cuenta 
@SpringBootTest
class CuentaTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SesionService sesionService;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Simula los otros servicios pq aun no existen 
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private Long idHuesped;
    private Long idAnfitrion;

    // Antes de cada prueba se limpia todo y se crean un huésped y un anfitrión
    @BeforeEach
    void prepararDatos() {
        sesionRepository.deleteAll();
        usuarioRepository.deleteAll();
        UsuarioResponse huesped = usuarioService.registrar(
                new RegistroRequest("Ana Pérez", "ana@correo.com", "clave12345", Rol.HUESPED));
        UsuarioResponse anfitrion = usuarioService.registrar(
                new RegistroRequest("Luis Gómez", "luis@correo.com", "clave12345", Rol.ANFITRION));
        idHuesped = huesped.getId();
        idAnfitrion = anfitrion.getId();
    }

    @Test
    @DisplayName("CA-016 sin reservas futuras la cuenta queda inactiva y la sesión se cierra")
    void desactivaSinReservas() {
        SesionResponse sesion = sesionService.iniciarSesion(new LoginRequest("ana@correo.com", "clave12345"));

        usuarioService.desactivarCuenta(idHuesped);

        assertEquals(EstadoUsuario.INACTIVO, usuarioService.consultarPerfil(idHuesped).getEstado());
        assertThrows(AutenticacionException.class, () -> sesionService.validarSesion(sesion.getToken()));
    }

    @Test
    @DisplayName("CA-016 con la cuenta inactiva no puede iniciar sesión")
    void inactivoNoPuedeEntrar() {
        usuarioService.desactivarCuenta(idHuesped);
        LoginRequest datos = new LoginRequest("ana@correo.com", "clave12345");

        assertThrows(AutenticacionException.class, () -> sesionService.iniciarSesion(datos));
    }

    @Test
    @DisplayName("CA-017 con reservas futuras no se puede desactivar")
    void noDesactivaConReservas() {
        // Se simula que Reservas responde que sí tiene reservas futuras
        when(serviciosExternos.tieneReservasFuturas(idHuesped)).thenReturn(true);

        CuentaException error = assertThrows(CuentaException.class, () -> usuarioService.desactivarCuenta(idHuesped));

        assertEquals("No puede desactivar la cuenta porque tiene reservas futuras confirmadas", error.getMessage());
        assertEquals(EstadoUsuario.ACTIVO, usuarioService.consultarPerfil(idHuesped).getEstado());
    }

    @Test
    @DisplayName("CA-018 al desactivar un anfitrión se piden pausar sus publicaciones")
    void anfitrionPausaPublicaciones() {
        usuarioService.desactivarCuenta(idAnfitrion);

        // Revisa que sí se le pidió a Alojamientos pausar las publicaciones
        verify(serviciosExternos).pausarPublicaciones(idAnfitrion);
    }

    @Test
    @DisplayName("CA-018 al desactivar un huésped no se pausa nada")
    void huespedNoPausaNada() {
        usuarioService.desactivarCuenta(idHuesped);

        verify(serviciosExternos, never()).pausarPublicaciones(idHuesped);
    }

    @Test
    @DisplayName("CA-019 al reactivar la cuenta vuelve a operar")
    void reactivaLaCuenta() {
        usuarioService.desactivarCuenta(idHuesped);

        SesionResponse sesion = sesionService.reactivarCuenta(new LoginRequest("ana@correo.com", "clave12345"));

        assertNotNull(sesion.getToken());
        assertEquals(EstadoUsuario.ACTIVO, usuarioService.consultarPerfil(idHuesped).getEstado());
    }

    @Test
    @DisplayName("CA-019 una cuenta restringida no se puede reactivar sola")
    void restringidaNoSeReactiva() {
        var usuario = usuarioRepository.findById(idHuesped).orElseThrow();
        usuario.setEstado(EstadoUsuario.RESTRINGIDO);
        usuarioRepository.save(usuario);
        LoginRequest datos = new LoginRequest("ana@correo.com", "clave12345");

        assertThrows(AutenticacionException.class, () -> sesionService.reactivarCuenta(datos));
    }
}