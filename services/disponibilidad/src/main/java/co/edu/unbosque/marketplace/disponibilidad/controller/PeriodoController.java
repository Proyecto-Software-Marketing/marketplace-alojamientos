package co.edu.unbosque.marketplace.disponibilidad.controller;

import co.edu.unbosque.marketplace.disponibilidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.disponibilidad.dto.PeriodoRequestDTO;
import co.edu.unbosque.marketplace.disponibilidad.dto.PeriodoResponseDTO;
import co.edu.unbosque.marketplace.disponibilidad.service.PeriodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU-16: el anfitrión registra, edita y elimina los periodos disponibles de su publicación
@RestController
@RequestMapping("/listings/{id}/availability")
public class PeriodoController {

    private final PeriodoService periodoService;
    private final ServiciosExternos serviciosExternos;

    public PeriodoController(PeriodoService periodoService, ServiciosExternos serviciosExternos) {
        this.periodoService = periodoService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-045
    @PostMapping
    public ResponseEntity<PeriodoResponseDTO> registrar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long idPublicacion,
            @Valid @RequestBody PeriodoRequestDTO solicitud) {
        serviciosExternos.verificarDuenoPublicacion(autorizacion, idPublicacion);

        PeriodoResponseDTO creado = PeriodoResponseDTO.desde(
                periodoService.registrar(idPublicacion, solicitud.fechaInicio(), solicitud.fechaFin()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // CA-046 y CA-047 al editar
    @PutMapping("/{periodId}")
    public ResponseEntity<PeriodoResponseDTO> editar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long idPublicacion,
            @PathVariable("periodId") Long idPeriodo,
            @Valid @RequestBody PeriodoRequestDTO solicitud) {
        serviciosExternos.verificarDuenoPublicacion(autorizacion, idPublicacion);

        return ResponseEntity.ok(PeriodoResponseDTO.desde(
                periodoService.editar(idPublicacion, idPeriodo, solicitud.fechaInicio(), solicitud.fechaFin())));
    }

    // CA-047 al eliminar
    @DeleteMapping("/{periodId}")
    public ResponseEntity<Void> eliminar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long idPublicacion,
            @PathVariable("periodId") Long idPeriodo) {
        serviciosExternos.verificarDuenoPublicacion(autorizacion, idPublicacion);

        periodoService.eliminar(idPublicacion, idPeriodo);
        return ResponseEntity.noContent().build();
    }
}