package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.ImagenesRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.ReglasRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.ServiciosRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.service.ContenidoPublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// HU-09: el anfitrión define los servicios, las reglas y las imágenes de su publicación.
// Rol requerido: ANFITRION. Cada PUT reemplaza la lista completa y responde la publicación (decisiones 1 y 2)
@RestController
public class ContenidoPublicacionController {

    private static final String SIN_PERMISO = "Solo el anfitrión puede modificar sus publicaciones";

    private final ContenidoPublicacionService contenidoPublicacionService;
    private final ServiciosExternos serviciosExternos;

    public ContenidoPublicacionController(ContenidoPublicacionService contenidoPublicacionService,
            ServiciosExternos serviciosExternos) {
        this.contenidoPublicacionService = contenidoPublicacionService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-026: agregar o quitar servicios (amenities)
    @PutMapping("/listings/{id}/amenities")
    public ResponseEntity<PublicacionResponseDTO> definirServicios(
            @RequestHeader("Authorization") String autorizacion, @PathVariable("id") Long id,
            @Valid @RequestBody ServiciosRequestDTO solicitud) {
        Long idAnfitrion = serviciosExternos.verificarAnfitrion(autorizacion, SIN_PERMISO);
        return ResponseEntity.ok(PublicacionResponseDTO.desde(
                contenidoPublicacionService.definirServicios(idAnfitrion, id, solicitud.servicios())));
    }

    // CA-027: registrar las reglas del alojamiento
    @PutMapping("/listings/{id}/rules")
    public ResponseEntity<PublicacionResponseDTO> definirReglas(
            @RequestHeader("Authorization") String autorizacion, @PathVariable("id") Long id,
            @Valid @RequestBody ReglasRequestDTO solicitud) {
        Long idAnfitrion = serviciosExternos.verificarAnfitrion(autorizacion, SIN_PERMISO);
        return ResponseEntity.ok(PublicacionResponseDTO.desde(
                contenidoPublicacionService.definirReglas(idAnfitrion, id, solicitud.reglas())));
    }

    // CA-028: agregar rutas o referencias de imágenes
    @PutMapping("/listings/{id}/images")
    public ResponseEntity<PublicacionResponseDTO> definirImagenes(
            @RequestHeader("Authorization") String autorizacion, @PathVariable("id") Long id,
            @Valid @RequestBody ImagenesRequestDTO solicitud) {
        Long idAnfitrion = serviciosExternos.verificarAnfitrion(autorizacion, SIN_PERMISO);
        return ResponseEntity.ok(PublicacionResponseDTO.desde(
                contenidoPublicacionService.definirImagenes(idAnfitrion, id, solicitud.imagenes())));
    }
}
