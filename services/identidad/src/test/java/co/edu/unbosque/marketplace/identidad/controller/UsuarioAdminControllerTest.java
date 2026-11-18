package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.service.SesionService;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class UsuarioAdminControllerTest {

    private UsuarioService usuarioService;
    private SesionService sesionService;
    private UsuarioAdminController controller;

    @BeforeEach
    void preparar() {
        usuarioService = mock(UsuarioService.class);
        sesionService = mock(SesionService.class);
        controller = new UsuarioAdminController(usuarioService, sesionService);
    }

    @Test
    void restringirUsuarioComoAdministrador() {
        UsuarioResponse administrador = mock(UsuarioResponse.class);
        UsuarioResponse respuesta = mock(UsuarioResponse.class);

        when(administrador.getId()).thenReturn(10L);

        when(sesionService.autorizar(
                "token-admin",
                java.util.List.of(Rol.ADMINISTRADOR),
                null
        )).thenReturn(administrador);

        when(usuarioService.restringirUsuario(25L, 10L))
                .thenReturn(respuesta);

        ResponseEntity<UsuarioResponse> resultado =
                controller.restringir("Bearer token-admin ", 25L);

        assertEquals(200, resultado.getStatusCode().value());
        assertSame(respuesta, resultado.getBody());

        verify(sesionService).autorizar(
                "token-admin",
                java.util.List.of(Rol.ADMINISTRADOR),
                null
        );

        verify(usuarioService).restringirUsuario(25L, 10L);
    }

    @Test
    void reactivarUsuarioComoAdministrador() {
        UsuarioResponse administrador = mock(UsuarioResponse.class);
        UsuarioResponse respuesta = mock(UsuarioResponse.class);

        when(administrador.getId()).thenReturn(10L);

        when(sesionService.autorizar(
                "token-admin",
                java.util.List.of(Rol.ADMINISTRADOR),
                null
        )).thenReturn(administrador);

        when(usuarioService.reactivarUsuario(25L, 10L))
                .thenReturn(respuesta);

        ResponseEntity<UsuarioResponse> resultado =
                controller.reactivar("Bearer token-admin", 25L);

        assertEquals(200, resultado.getStatusCode().value());
        assertSame(respuesta, resultado.getBody());

        verify(sesionService).autorizar(
                "token-admin",
                java.util.List.of(Rol.ADMINISTRADOR),
                null
        );

        verify(usuarioService).reactivarUsuario(25L, 10L);
    }
    
    @Test
    void listarUsuariosComoAdministrador() {
        when(usuarioService.listarUsuarios()).thenReturn(java.util.List.of());

        ResponseEntity<java.util.List<UsuarioResponse>> resultado = controller.listar("Bearer token-admin");

        assertEquals(200, resultado.getStatusCode().value());
        verify(sesionService).autorizar("token-admin", java.util.List.of(Rol.ADMINISTRADOR), null);
    }
}