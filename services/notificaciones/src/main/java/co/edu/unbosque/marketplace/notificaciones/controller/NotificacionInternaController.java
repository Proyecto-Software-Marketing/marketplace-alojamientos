package co.edu.unbosque.marketplace.notificaciones.controller;

import co.edu.unbosque.marketplace.notificaciones.dto.NotificacionRequestDTO;
import co.edu.unbosque.marketplace.notificaciones.dto.NotificacionResponseDTO;
import co.edu.unbosque.marketplace.notificaciones.service.NotificacionService;
import co.edu.unbosque.marketplace.notificaciones.service.RegistroNotificacion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU-30: ruta interna que usan Alojamientos y Reservas para avisar un evento.
// Igual que las rutas internas de Disponibilidad, no pide token (solo es accesible dentro de la red de Docker).
@RestController
@RequestMapping("/internal/notifications")
public class NotificacionInternaController {

    private final NotificacionService notificacionService;

    public NotificacionInternaController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // CA-083 y CA-084. 201 si se creó; 200 si era un reintento y ya existía (decisión 3)
    @PostMapping
    public ResponseEntity<NotificacionResponseDTO> registrar(@Valid @RequestBody NotificacionRequestDTO solicitud) {
        RegistroNotificacion registro = notificacionService.registrar(solicitud.usuarioId(), solicitud.tipoEvento(),
                solicitud.entidadTipo(), solicitud.entidadId(), solicitud.mensaje());

        HttpStatus estado = registro.nueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(NotificacionResponseDTO.desde(registro.notificacion()));
    }
}
