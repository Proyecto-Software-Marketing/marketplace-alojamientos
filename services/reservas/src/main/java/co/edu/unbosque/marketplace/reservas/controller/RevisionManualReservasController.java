package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.service.RevisionManualReservasService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RevisionManualReservasController {

    private final RevisionManualReservasService revisionManualService;

    public RevisionManualReservasController(
            RevisionManualReservasService revisionManualService) {
        this.revisionManualService = revisionManualService;
    }

    // CA-095: pasar a revision manual las reservas afectadas.
    @PostMapping("/internal/bookings/review-manual")
    public ResponseEntity<ResultadoRevisionManual> revisar(
            @Valid @RequestBody SolicitudRevisionManual solicitud) {

        int cantidad = revisionManualService.revisarReservas(
                solicitud.publicacionId());

        return ResponseEntity.ok(
                new ResultadoRevisionManual(
                        solicitud.publicacionId(),
                        cantidad));
    }

    public record SolicitudRevisionManual(
            @NotNull @Positive Long publicacionId) {
    }

    public record ResultadoRevisionManual(
            Long publicacionId,
            int reservasActualizadas) {
    }
}