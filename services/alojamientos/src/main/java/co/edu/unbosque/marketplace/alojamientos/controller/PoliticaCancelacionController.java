package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.AsociacionPoliticaDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.ListaPoliticasDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PoliticaCancelacionDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.service.PoliticaCancelacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PoliticaCancelacionController {

    private final PoliticaCancelacionService politicaService;
    private final ServiciosExternos serviciosExternos;

    public PoliticaCancelacionController(
            PoliticaCancelacionService politicaService,
            ServiciosExternos serviciosExternos) {
        this.politicaService = politicaService;
        this.serviciosExternos = serviciosExternos;
    }

    // HU-11: consultar las políticas disponibles
    @GetMapping("/cancellation-policies")
    public ResponseEntity<ListaPoliticasDTO> listar() {
        return ResponseEntity.ok(new ListaPoliticasDTO(
                politicaService.listar().stream()
                        .map(PoliticaCancelacionDTO::desde)
                        .toList()));
    }

    // HU-11: el anfitrión asocia una política a su publicación
    @PutMapping("/listings/{id}/cancellation-policy")
    public ResponseEntity<PublicacionResponseDTO> asociar(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long id,
            @Valid @RequestBody AsociacionPoliticaDTO solicitud) {

        Long anfitrionId = serviciosExternos.verificarAnfitrion(
                autorizacion,
                "Solo el anfitrion puede modificar sus publicaciones");

        return ResponseEntity.ok(PublicacionResponseDTO.desde(
                politicaService.asociar(id, anfitrionId, solicitud.politicaId())));
    }

    // Consulta interna que utilizará el microservicio de Reservas
    @GetMapping("/internal/listings/{id}/cancellation-policy")
    public ResponseEntity<PoliticaCancelacionDTO> consultarInterna(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(PoliticaCancelacionDTO.desde(
                politicaService.consultarInterna(id)));
    }
}