package co.edu.unbosque.marketplace.resenas.controller;

import co.edu.unbosque.marketplace.resenas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.resenas.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.resenas.dto.OcultarResenaRequestDTO;
import co.edu.unbosque.marketplace.resenas.dto.ResenaResponseDTO;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.service.ModeracionResenasService;
import co.edu.unbosque.marketplace.resenas.service.ResultadoOcultamiento;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// HU-36: el administrador oculta reseñas que incumplen las políticas
@RestController
public class ModeracionResenaController {

    private final ModeracionResenasService moderacionResenasService;
    private final ServiciosExternos serviciosExternos;

    public ModeracionResenaController(ModeracionResenasService moderacionResenasService,
            ServiciosExternos serviciosExternos) {
        this.moderacionResenasService = moderacionResenasService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-100 y CA-101. Rol requerido: ADMINISTRADOR
    @PostMapping("/admin/reviews/{id}/hide")
    public ResponseEntity<ResenaResponseDTO> ocultar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long resenaId, @Valid @RequestBody OcultarResenaRequestDTO solicitud) {
        Long idAdministrador = serviciosExternos.verificarAdministrador(autorizacion);

        ResultadoOcultamiento resultado = moderacionResenasService.ocultar(idAdministrador, resenaId,
                solicitud.motivo());
        Resena resena = resultado.resena();

        // CA-101 y decisión 6: el evento se envía después de guardar; si Auditoría falla la reseña sigue oculta
        if (resultado.recienOcultada()) {
            serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(idAdministrador, "RESENA", resena.getId(),
                    "OCULTAR_RESENA", "EXITO", resena.getMotivoOcultamiento()));
        }
        return ResponseEntity.ok(ResenaResponseDTO.desde(resena));
    }
}
