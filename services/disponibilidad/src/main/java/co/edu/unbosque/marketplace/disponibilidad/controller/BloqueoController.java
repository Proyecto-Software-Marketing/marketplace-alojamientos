package co.edu.unbosque.marketplace.disponibilidad.controller;

import co.edu.unbosque.marketplace.disponibilidad.dto.BloqueoRequestDTO;
import co.edu.unbosque.marketplace.disponibilidad.dto.ConsultaDisponibilidadDTO;
import co.edu.unbosque.marketplace.disponibilidad.dto.DisponibilidadResultadoDTO;
import co.edu.unbosque.marketplace.disponibilidad.service.BloqueoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU-18: rutas internas que usa Reservas (solo dentro de la red de Docker, sin token: decisión 6)
@RestController
@RequestMapping("/internal/availability")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    public BloqueoController(BloqueoService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    // CA-050: Reservas avisa que confirmó una reserva
    @PostMapping("/block")
    public ResponseEntity<Void> bloquear(@Valid @RequestBody BloqueoRequestDTO solicitud) {
        bloqueoService.bloquear(solicitud.publicacionId(), solicitud.reservaId(),
                solicitud.fechaLlegada(), solicitud.fechaSalida());
        return ResponseEntity.noContent().build();
    }

    // Decisión 4: Reservas avisa que se canceló una reserva
    @PostMapping("/release")
    public ResponseEntity<Void> liberar(@Valid @RequestBody BloqueoRequestDTO solicitud) {
        bloqueoService.liberar(solicitud.reservaId());
        return ResponseEntity.noContent().build();
    }

    // CA-052: Búsqueda, Cotización y Reservas preguntan qué publicaciones tienen libres unas fechas
    @PostMapping("/check")
    public ResponseEntity<DisponibilidadResultadoDTO> consultar(@Valid @RequestBody ConsultaDisponibilidadDTO consulta) {
        return ResponseEntity.ok(new DisponibilidadResultadoDTO(
                bloqueoService.disponibles(consulta.publicacionIds(), consulta.fechaLlegada(), consulta.fechaSalida())));
    }
}
