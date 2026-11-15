package co.edu.unbosque.marketplace.notificaciones.controller;

import co.edu.unbosque.marketplace.notificaciones.client.ServiciosExternos;
import co.edu.unbosque.marketplace.notificaciones.dto.NotificacionListaDTO;
import co.edu.unbosque.marketplace.notificaciones.dto.NotificacionResponseDTO;
import co.edu.unbosque.marketplace.notificaciones.model.Notificacion;
import co.edu.unbosque.marketplace.notificaciones.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// HU-31: el usuario con sesión consulta sus notificaciones y las marca como leídas
@RestController
@RequestMapping("/notifications")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final ServiciosExternos serviciosExternos;

    public NotificacionController(NotificacionService notificacionService, ServiciosExternos serviciosExternos) {
        this.notificacionService = notificacionService;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-085 y CA-087
    @GetMapping
    public ResponseEntity<NotificacionListaDTO> listar(@RequestHeader("Authorization") String autorizacion) {
        Long idUsuario = serviciosExternos.verificarSesion(autorizacion);

        List<Notificacion> notificaciones = notificacionService.listar(idUsuario);
        long noLeidas = notificaciones.stream().filter(notificacion -> !notificacion.isLeida()).count();
        return ResponseEntity.ok(new NotificacionListaDTO(
                notificaciones.stream().map(NotificacionResponseDTO::desde).toList(), noLeidas));
    }

    // CA-086 y CA-087
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificacionResponseDTO> marcarLeida(@RequestHeader("Authorization") String autorizacion,
            @PathVariable("id") Long idNotificacion) {
        Long idUsuario = serviciosExternos.verificarSesion(autorizacion);

        return ResponseEntity.ok(NotificacionResponseDTO.desde(
                notificacionService.marcarLeida(idUsuario, idNotificacion)));
    }
}
