package co.edu.unbosque.marketplace.identidad.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.identidad.dto.EstadisticasUsuariosResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.service.RecuperacionService;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;


@RestController
@RequestMapping("/interno/usuarios")
public class InternoController {

    private final UsuarioService usuarioService;
    private final RecuperacionService recuperacionService;

    public InternoController(UsuarioService usuarioService, RecuperacionService recuperacionService) {
        this.usuarioService = usuarioService;
        this.recuperacionService = recuperacionService;
    }

    //Reportes para el dashboard
    @GetMapping("/estadisticas")
    public EstadisticasUsuariosResponse estadisticas() {
        return usuarioService.estadisticas();
    }

    // HU-04 Notificaciones busca el usuario del correo para el buzón simulado de recuperación
    @GetMapping("/id")
    public ResponseEntity<UsuarioResponse> buscarPorCorreo(@RequestParam String correo) {
        return ResponseEntity.of(recuperacionService.buscarPorCorreo(correo));
    }
}