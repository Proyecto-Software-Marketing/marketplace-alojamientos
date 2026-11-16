package co.edu.unbosque.marketplace.auditoria.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaRequestDTO;
import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaResponseDTO;
import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;
import co.edu.unbosque.marketplace.auditoria.service.AuditoriaService;
import jakarta.validation.Valid;

@RestController
public class AuditoriaInternaController {

	private final AuditoriaService servicio;

	public AuditoriaInternaController(AuditoriaService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/internal/audit-events")
	public ResponseEntity<EventoAuditoriaResponseDTO> registrar(
			@Valid @RequestBody EventoAuditoriaRequestDTO solicitud) {
		EventoAuditoria evento = servicio.registrar(solicitud);
		return ResponseEntity.status(HttpStatus.CREATED).body(EventoAuditoriaResponseDTO.desde(evento));
	}

}
