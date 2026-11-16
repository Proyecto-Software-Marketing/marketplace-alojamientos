package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.RecuperacionRequest;
import co.edu.unbosque.marketplace.identidad.dto.RestablecerRequest;
import co.edu.unbosque.marketplace.identidad.service.RecuperacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU-04 recuperación simulada de contraseña, sin sesión porque el usuario no puede entrar
@RestController
@RequestMapping("/autenticacion")
public class RecuperacionController {

    private final RecuperacionService recuperacionService;

    public RecuperacionController(RecuperacionService recuperacionService) {
        this.recuperacionService = recuperacionService;
    }

    // CA-010 siempre responde 204, exista o no el correo
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<Void> recuperarContrasena(@Valid @RequestBody RecuperacionRequest datos) {
        recuperacionService.solicitar(datos);
        return ResponseEntity.noContent().build();
    }

    // CA-011 y CA-012
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Void> restablecerContrasena(@Valid @RequestBody RestablecerRequest datos) {
        recuperacionService.restablecer(datos);
        return ResponseEntity.noContent().build();
    }
}
