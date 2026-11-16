package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.dto.DatosCotizacionDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PropietarioPublicacionDTO;
import co.edu.unbosque.marketplace.alojamientos.service.PublicacionInternaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Rutas internas /internal/listings que llaman Disponibilidad, Reservas y Cotización
// (solo dentro de la red de Docker, sin token)
@RestController
public class ListingInternoController {

    private final PublicacionInternaService publicacionInternaService;

    public ListingInternoController(PublicacionInternaService publicacionInternaService) {
        this.publicacionInternaService = publicacionInternaService;
    }

    // Decisión 11: dueño y estado de la publicación, en cualquier estado
    @GetMapping("/internal/listings/{id}/owner")
    public ResponseEntity<PropietarioPublicacionDTO> propietario(@PathVariable("id") Long id) {
        return ResponseEntity.ok(PropietarioPublicacionDTO.desde(publicacionInternaService.buscar(id)));
    }

    // HU-10, decisión 13: datos para calcular la cotización (CA-029, CA-030 y CA-129), en cualquier estado
    @GetMapping("/internal/listings/{id}/quote-data")
    public ResponseEntity<DatosCotizacionDTO> datosCotizacion(@PathVariable("id") Long id) {
        return ResponseEntity.ok(DatosCotizacionDTO.desde(publicacionInternaService.buscar(id)));
    }
}
