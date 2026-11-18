package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.service.SesionService;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;
    private final SesionService sesionService;

    public UsuarioAdminController(UsuarioService usuarioService, SesionService sesionService) {
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
    }
 @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
            @RequestHeader("Authorization") String autorizacion) {

        sesionService.autorizar(extraerToken(autorizacion), List.of(Rol.ADMINISTRADOR), null);
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }
    @PostMapping("/{id}/restringir")
    public ResponseEntity<UsuarioResponse> restringir(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id) {

        Long administradorId = sesionService.autorizar(
                extraerToken(autorizacion),
                List.of(Rol.ADMINISTRADOR),
                null).getId();

        return ResponseEntity.ok(usuarioService.restringirUsuario(id, administradorId));
    }

    @PostMapping("/{id}/reactivar")
    public ResponseEntity<UsuarioResponse> reactivar(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id) {

        Long administradorId = sesionService.autorizar(
                extraerToken(autorizacion),
                List.of(Rol.ADMINISTRADOR),
                null).getId();

        return ResponseEntity.ok(usuarioService.reactivarUsuario(id, administradorId));
    }

    private String extraerToken(String autorizacion) {
        return autorizacion.replace("Bearer ", "").trim();
    }
}