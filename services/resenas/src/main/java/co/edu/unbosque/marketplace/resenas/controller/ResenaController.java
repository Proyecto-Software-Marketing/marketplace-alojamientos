package co.edu.unbosque.marketplace.resenas.controller;

import co.edu.unbosque.marketplace.resenas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.resenas.dto.ResenaRequestDTO;
import co.edu.unbosque.marketplace.resenas.dto.ResenaResponseDTO;
import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// HU-32: el huésped califica y reseña una estancia completada
@RestController
public class ResenaController {

    private final ResenaService resenaService;
    private final ServiciosExternos serviciosExternos;

    public ResenaController(ResenaService resenaService, ServiciosExternos serviciosExternos) {
        this.resenaService = resenaService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-088 a CA-091. Rol requerido: HUESPED
    @PostMapping("/bookings/{id}/review")
    public ResponseEntity<ResenaResponseDTO> resenar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long reservaId, @Valid @RequestBody ResenaRequestDTO solicitud) {
        Long idHuesped = serviciosExternos.verificarHuesped(autorizacion);
        ReservaDTO reserva = serviciosExternos.consultarReserva(autorizacion, reservaId);

        Resena resena = resenaService.registrar(idHuesped, reserva, solicitud.calificacionEntera(),
                solicitud.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResenaResponseDTO.desde(resena));
    }
}
