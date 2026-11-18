package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.client.IntegracionesBloqueo;
import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.BloqueoPublicacionRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.service.BloqueoPublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BloqueoPublicacionController {

    private final BloqueoPublicacionService bloqueoService;
    private final ServiciosExternos serviciosExternos;
    private final IntegracionesBloqueo integracionesBloqueo;

    public BloqueoPublicacionController(
            BloqueoPublicacionService bloqueoService,
            ServiciosExternos serviciosExternos,
            IntegracionesBloqueo integracionesBloqueo) {

        this.bloqueoService = bloqueoService;
        this.serviciosExternos = serviciosExternos;
        this.integracionesBloqueo = integracionesBloqueo;
    }

    // HU-34: solo un administrador puede bloquear publicaciones.
    @PostMapping("/admin/listings/{id}/block")
    public ResponseEntity<PublicacionResponseDTO> bloquear(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id,
            @Valid @RequestBody BloqueoPublicacionRequestDTO solicitud) {

        Long administradorId =
                serviciosExternos.verificarAdministrador(autorizacion);

        // Guarda el bloqueo.
        Publicacion publicacion =
                bloqueoService.bloquear(id, solicitud.motivo());

        // CA-095: solicitar revision manual de reservas afectadas.
        integracionesBloqueo.solicitarRevisionManual(
                publicacion.getId());

        // CA-096: notificar al anfitrion.
        integracionesBloqueo.notificarBloqueo(
                publicacion.getAnfitrionId(),
                publicacion.getId(),
                publicacion.getMotivoBloqueo());

        // CA-096: registrar auditoria.
        integracionesBloqueo.registrarAuditoria(
                administradorId,
                publicacion.getId(),
                publicacion.getMotivoBloqueo());

        return ResponseEntity.ok(
                PublicacionResponseDTO.desde(publicacion));
    }
}