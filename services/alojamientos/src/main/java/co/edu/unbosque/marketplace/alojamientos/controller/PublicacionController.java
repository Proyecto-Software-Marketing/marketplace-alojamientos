package co.edu.unbosque.marketplace.alojamientos.controller;

import co.edu.unbosque.marketplace.alojamientos.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.alojamientos.client.ServiciosExternos;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionListaResponseDTO;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.service.PublicacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import co.edu.unbosque.marketplace.alojamientos.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.RechazarPublicacionRequestDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;


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

// HU-12 (CA-035 y CA-036): enviar una publicacion a revision.
@PostMapping("/listings/{id}/submit")
public ResponseEntity<PublicacionResponseDTO> enviarARevision(
        @RequestHeader("Authorization") String autorizacion,
        @PathVariable Long id) {

    Long anfitrionId = serviciosExternos.verificarAnfitrion(autorizacion);

    Publicacion publicacion = publicacionService.enviarARevision(
            id, anfitrionId);

    return ResponseEntity.ok(PublicacionResponseDTO.desde(publicacion));
}

    // HU-08: consultar una publicacion (propietario o administrador).
    @GetMapping("/listings/{id}")
    public ResponseEntity<PublicacionResponseDTO> consultarPorId(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id) {

        Publicacion publicacion = publicacionService.consultarPorId(id);

        serviciosExternos.verificarAdministradorOAnfitrion(
                autorizacion, publicacion.getAnfitrionId());

        return ResponseEntity.ok(PublicacionResponseDTO.desde(publicacion));
    }
    // HU-13: listar publicaciones por estado.
    @GetMapping("/admin/listings")
    public ResponseEntity<PublicacionListaResponseDTO> listarAdministracion(
            @RequestHeader("Authorization") String autorizacion,
            @RequestParam(required = false) EstadoPublicacion estado) {

        serviciosExternos.verificarAdministrador(autorizacion);

        var publicaciones = publicacionService.listarPorEstado(estado)
                .stream()
                .map(PublicacionResponseDTO::desde)
                .toList();

        return ResponseEntity.ok(
                new PublicacionListaResponseDTO(publicaciones));
    }
    // HU-13 (CA-037): el administrador aprueba una publicacion.
    @PostMapping("/admin/listings/{id}/approve")
    public ResponseEntity<PublicacionResponseDTO> aprobar(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id) {

Long administradorId = serviciosExternos.verificarAdministrador(autorizacion);
Publicacion publicacion = publicacionService.aprobar(id);

// Registrar la aprobacion en Auditoria
serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(
        administradorId,
        "PUBLICACION",
        publicacion.getId(),
        "APROBAR_PUBLICACION",
        "EXITO",
        "El administrador aprobo la publicacion"
));

// Notificar al anfitrion
serviciosExternos.notificarPublicacionAprobada(
        publicacion.getAnfitrionId(),
        publicacion.getId()
);

return ResponseEntity.ok(PublicacionResponseDTO.desde(publicacion));
    }
// HU-13 (CA-038): el administrador rechaza una publicacion.
@PostMapping("/admin/listings/{id}/reject")
public ResponseEntity<PublicacionResponseDTO> rechazar(
        @RequestHeader("Authorization") String autorizacion,
        @PathVariable Long id,
        @Valid @RequestBody RechazarPublicacionRequestDTO solicitud) {

    Long administradorId = serviciosExternos.verificarAdministrador(autorizacion);

    Publicacion publicacion = publicacionService.rechazar(
            id, solicitud.motivo());

    // Registrar el rechazo en Auditoria
    serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(
            administradorId,
            "PUBLICACION",
            publicacion.getId(),
            "RECHAZAR_PUBLICACION",
            "EXITO",
                "Motivo: " + ServiciosExternos.recortar(publicacion.getMotivoRechazo(), 1900)
    ));
    // HU-13 (CA-039): notificar al anfitrion el motivo del rechazo.
serviciosExternos.notificarPublicacionRechazada(
        publicacion.getAnfitrionId(),
        publicacion.getId(),
        publicacion.getMotivoRechazo()
);
    return ResponseEntity.ok(PublicacionResponseDTO.desde(publicacion));
}
// HU-14 (CA-040): pausar una publicacion activa.
// Rol requerido: ANFITRION.
@PostMapping("/listings/{id}/pause")
public ResponseEntity<PublicacionResponseDTO> pausar(
        @RequestHeader("Authorization") String autorizacion,
        @PathVariable Long id) {

    Long anfitrionId = serviciosExternos.verificarAnfitrion(autorizacion);

    Publicacion publicacion = publicacionService.pausar(id, anfitrionId);

    return ResponseEntity.ok(PublicacionResponseDTO.desde(publicacion));
}

// HU-14 (CA-041 y CA-042): reactivar una publicacion pausada.
// No se permite reactivar publicaciones BLOQUEADA.
@PostMapping("/listings/{id}/resume")
public ResponseEntity<PublicacionResponseDTO> reactivar(
        @RequestHeader("Authorization") String autorizacion,
        @PathVariable Long id) {

    Long anfitrionId = serviciosExternos.verificarAnfitrion(autorizacion);

    Publicacion publicacion = publicacionService.reactivar(id, anfitrionId);

    return ResponseEntity.ok(PublicacionResponseDTO.desde(publicacion));
}


    // HU-15: consultar las publicaciones del anfitrion autenticado.
    @GetMapping("/listings/mine")
    public ResponseEntity<PublicacionListaResponseDTO> listarMisPublicaciones(
            @RequestHeader("Authorization") String autorizacion) {

        Long anfitrionId = serviciosExternos.verificarAnfitrion(autorizacion);

        var publicaciones = publicacionService.listarMisPublicaciones(anfitrionId)
                .stream()
                .map(PublicacionResponseDTO::desde)
                .toList();

        return ResponseEntity.ok(
                new PublicacionListaResponseDTO(publicaciones));
    }

    // HU-08: editar una publicacion existente.
    @PutMapping("/listings/{id}")
    public ResponseEntity<PublicacionResponseDTO> editar(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id,
            @Valid @RequestBody PublicacionRequestDTO solicitud) {

        UsuarioIdentidadDTO usuario =
                serviciosExternos.verificarEditor(autorizacion);

        boolean esAdministrador =
                "ADMINISTRADOR".equals(usuario.rol());

        Publicacion publicacion = publicacionService.editar(
                id, usuario.id(), esAdministrador, solicitud);

        if (esAdministrador) {
            serviciosExternos.registrarAuditoriaEdicion(
                    usuario.id(), publicacion.getId());
        }

        return ResponseEntity.ok(
                PublicacionResponseDTO.desde(publicacion));
    }
}
