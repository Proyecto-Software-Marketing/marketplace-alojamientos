package co.edu.unbosque.marketplace.reservas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.reservas.dto.EstadisticasReservasDTO;
import co.edu.unbosque.marketplace.reservas.dto.StayClosureResultDTO;
import co.edu.unbosque.marketplace.reservas.service.ConsultasInternasService;
import co.edu.unbosque.marketplace.reservas.service.StayClosureService;

// Rutas internas de Reservas: sin token, solo dentro de la red de Docker Compose
@RestController
public class InternalBookingController {

    private final StayClosureService stayClosureService;
    private final ConsultasInternasService consultasInternasService;

    public InternalBookingController(StayClosureService stayClosureService,
                                     ConsultasInternasService consultasInternasService) {
        this.stayClosureService = stayClosureService;
        this.consultasInternasService = consultasInternasService;
    }

    // HU-27: ejecuta el cierre de estancias a demanda 
    @PostMapping("/internal/bookings/close-stays")
    public ResponseEntity<StayClosureResultDTO> cerrarEstancias() {
        return ResponseEntity.ok(stayClosureService.cerrarEstanciasTerminadas());
    }

    // HU-06: la usa Identidad al desactivar una cuenta 
    @GetMapping("/interno/reservas/futuras")
    public boolean tieneReservasFuturas(@RequestParam Long idUsuario) {
        return consultasInternasService.tieneReservasFuturas(idUsuario);
    }

    // HU-39: la usa Reportes para el dashboard
    @GetMapping("/interno/reservas/estadisticas")
    public EstadisticasReservasDTO estadisticas() {
        return consultasInternasService.estadisticas();
    }
}