package co.edu.unbosque.marketplace.disponibilidad.controller;

import co.edu.unbosque.marketplace.disponibilidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.disponibilidad.dto.CalendarioResponseDTO;
import co.edu.unbosque.marketplace.disponibilidad.service.CalendarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU-17: cualquier usuario con sesión consulta el calendario de una publicación
@RestController
@RequestMapping("/listings/{id}/calendar")
public class CalendarioController {

    private final CalendarioService calendarioService;
    private final ServiciosExternos serviciosExternos;

    public CalendarioController(CalendarioService calendarioService, ServiciosExternos serviciosExternos) {
        this.calendarioService = calendarioService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-048 y CA-049
    @GetMapping
    public ResponseEntity<CalendarioResponseDTO> consultar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long idPublicacion) {
        serviciosExternos.verificarSesion(autorizacion);
        String estadoPublicacion = serviciosExternos.consultarEstadoPublicacion(idPublicacion);

        return ResponseEntity.ok(new CalendarioResponseDTO(idPublicacion,
                calendarioService.construir(idPublicacion, estadoPublicacion)));
    }
}