package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.PerfilRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.service.SesionService;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Rutas del perfil del usuario que tiene la sesión abierta
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final SesionService sesionService;

    public UsuarioController(UsuarioService usuarioService, SesionService sesionService) {
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
    }

    // CA-013
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> consultarPerfil(@RequestHeader("Authorization") String autorizacion) {
        Long idUsuario = sesionService.validarSesion(extraerToken(autorizacion)).getId();
        return ResponseEntity.ok(usuarioService.consultarPerfil(idUsuario));
    }

    // CA-014 y CA-015
    @PutMapping("/perfil")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(@RequestHeader("Authorization") String autorizacion,
            @Valid @RequestBody PerfilRequest datos) {
        Long idUsuario = sesionService.validarSesion(extraerToken(autorizacion)).getId();
        return ResponseEntity.ok(usuarioService.actualizarPerfil(idUsuario, datos));
    }

    // Quita la palabra "Bearer " para dejar solo el token
    private String extraerToken(String autorizacion) {
        return autorizacion.replace("Bearer ", "").trim();
    }
}