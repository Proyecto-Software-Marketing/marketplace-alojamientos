package co.edu.unbosque.marketplace.resenas.controller;

import co.edu.unbosque.marketplace.resenas.service.ConsultaResenasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Rutas internas que usan otros servicios (solo dentro de la red de Docker, sin token)
@RestController
public class ResenaInternaController {

    private final ConsultaResenasService consultaResenasService;

    public ResenaInternaController(ConsultaResenasService consultaResenasService) {
        this.consultaResenasService = consultaResenasService;
    }

    // HU-33 (decisión 6): Búsqueda pide el promedio de varias publicaciones, sin reseñas ocultas
    @GetMapping("/internal/reviews/averages")
    public ResponseEntity<Map<Long, Double>> promedios(@RequestParam("listingIds") String listingIds) {
        return ResponseEntity.ok(consultaResenasService.promedios(listingIds));
    }
}
