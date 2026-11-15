package co.edu.unbosque.marketplace.identidad.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.identidad.dto.EstadisticasUsuariosResponse;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;


@RestController
@RequestMapping("/interno/usuarios")
public class InternoController {

    private final UsuarioService usuarioService;

    public InternoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    //Reportes para el dashboard
    @GetMapping("/estadisticas")
    public EstadisticasUsuariosResponse estadisticas() {
        return usuarioService.estadisticas();
    }
}