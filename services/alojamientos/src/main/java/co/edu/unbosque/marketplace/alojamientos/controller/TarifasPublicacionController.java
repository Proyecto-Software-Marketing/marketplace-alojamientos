package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.TarifasRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.service.TarifasPublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// HU-10: el anfitrión define la tarifa de limpieza, el descuento por estancia larga y los servicios adicionales.
// Rol requerido: ANFITRION. Reemplaza toda la configuración y responde la publicación (decisión 1)
@RestController
public class TarifasPublicacionController {

    private static final String SIN_PERMISO = "Solo el anfitrión puede modificar sus publicaciones";

    private final TarifasPublicacionService tarifasPublicacionService;
    private final ServiciosExternos serviciosExternos;

    public TarifasPublicacionController(TarifasPublicacionService tarifasPublicacionService,
            ServiciosExternos serviciosExternos) {
        this.tarifasPublicacionService = tarifasPublicacionService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-029, CA-030, CA-031 y CA-129
    @PutMapping("/listings/{id}/pricing")
    public ResponseEntity<PublicacionResponseDTO> definirTarifas(
            @RequestHeader("Authorization") String autorizacion, @PathVariable("id") Long id,
            @Valid @RequestBody TarifasRequestDTO solicitud) {
        Long idAnfitrion = serviciosExternos.verificarAnfitrion(autorizacion, SIN_PERMISO);
        return ResponseEntity.ok(PublicacionResponseDTO.desde(
                tarifasPublicacionService.definirTarifas(idAnfitrion, id, solicitud)));
    }
}
