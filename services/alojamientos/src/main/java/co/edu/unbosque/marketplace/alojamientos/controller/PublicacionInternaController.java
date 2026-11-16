package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.dto.EstadisticasPublicacionesDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionDetalleDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResumenDTO;
import co.edu.unbosque.marketplace.alojamientos.service.PublicacionInternaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Rutas internas /interno/publicaciones que ya llaman Búsqueda, Reportes e Identidad
// (solo dentro de la red de Docker, sin token). Se respetan sus rutas y sus nombres de campos
@RestController
public class PublicacionInternaController {

    private final PublicacionInternaService publicacionInternaService;

    public PublicacionInternaController(PublicacionInternaService publicacionInternaService) {
        this.publicacionInternaService = publicacionInternaService;
    }

    // Decisión 12: Búsqueda pide las publicaciones ACTIVA de una ciudad. Responde un arreglo
    @GetMapping("/interno/publicaciones/activas")
    public ResponseEntity<List<PublicacionResumenDTO>> activas(
            @RequestParam(name = "ciudad", required = false) String ciudad) {
        return ResponseEntity.ok(publicacionInternaService.activas(ciudad).stream()
                .map(PublicacionResumenDTO::desde)
                .toList());
    }

    // Decisión 14: Reportes pide cuántas publicaciones están ACTIVA
    @GetMapping("/interno/publicaciones/estadisticas")
    public ResponseEntity<EstadisticasPublicacionesDTO> estadisticas() {
        return ResponseEntity.ok(new EstadisticasPublicacionesDTO(publicacionInternaService.contarActivas()));
    }

    // Decisión 13: Búsqueda pide el detalle de una publicación (en cualquier estado; Búsqueda filtra ACTIVA)
    @GetMapping("/interno/publicaciones/{id}")
    public ResponseEntity<PublicacionDetalleDTO> detalle(@PathVariable("id") Long id) {
        return ResponseEntity.ok(PublicacionDetalleDTO.desde(publicacionInternaService.buscar(id)));
    }

    // Decisión 15: Identidad pide pausar las publicaciones del anfitrión al desactivar su cuenta (HU-06, CA-018)
    @PostMapping("/interno/publicaciones/pausar")
    public ResponseEntity<Void> pausar(@RequestParam("idAnfitrion") Long idAnfitrion) {
        publicacionInternaService.pausarPorAnfitrion(idAnfitrion);
        return ResponseEntity.noContent().build();
    }
}
