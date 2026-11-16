package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.service.PublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// HU-07: el anfitrión crea una publicación en borrador
@RestController
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final ServiciosExternos serviciosExternos;

    public PublicacionController(PublicacionService publicacionService, ServiciosExternos serviciosExternos) {
        this.publicacionService = publicacionService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-020 a CA-022. Rol requerido: ANFITRION
    @PostMapping("/listings")
    public ResponseEntity<PublicacionResponseDTO> crear(@RequestHeader("Authorization") String autorizacion,
            @Valid @RequestBody PublicacionRequestDTO solicitud) {
        Long idAnfitrion = serviciosExternos.verificarAnfitrion(autorizacion);

        Publicacion publicacion = publicacionService.crear(idAnfitrion, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(PublicacionResponseDTO.desde(publicacion));
    }
}
