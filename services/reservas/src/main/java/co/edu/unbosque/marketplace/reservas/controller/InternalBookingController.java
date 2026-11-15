package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.dto.StayClosureResultDTO;
import co.edu.unbosque.marketplace.reservas.service.StayClosureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// Rutas internas de Reservas (/internal/*): sin token, solo dentro de la red de Docker Compose
@RestController
public class InternalBookingController {

    private final StayClosureService stayClosureService;

    public InternalBookingController(StayClosureService stayClosureService) {
        this.stayClosureService = stayClosureService;
    }

    // HU-27: ejecuta el cierre de estancias a demanda (el mismo que corre la tarea diaria)
    @PostMapping("/internal/bookings/close-stays")
    public ResponseEntity<StayClosureResultDTO> cerrarEstancias() {
        return ResponseEntity.ok(stayClosureService.cerrarEstanciasTerminadas());
    }
}
