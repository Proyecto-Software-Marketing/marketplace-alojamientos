package co.edu.unbosque.marketplace.auditoria.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.auditoria.client.ServiciosExternos;
import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaListaDTO;
import co.edu.unbosque.marketplace.auditoria.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.marketplace.auditoria.service.AuditoriaService;
import co.edu.unbosque.marketplace.auditoria.exception.SolicitudInvalidaException;

// Consulta de la auditoria para el administrador (HU-41, CA-114)
@RestController
@RequestMapping("/admin")
public class AuditoriaAdminController {

	private final AuditoriaService servicio;
	private final ServiciosExternos serviciosExternos;

	public AuditoriaAdminController(AuditoriaService servicio, ServiciosExternos serviciosExternos) {
		this.servicio = servicio;
		this.serviciosExternos = serviciosExternos;
	}

	@GetMapping("/audit-events")
	public ResponseEntity<EventoAuditoriaListaDTO> consultar(
			@RequestHeader(value = "Authorization", required = false) String autorizacion,
			@RequestParam(required = false) Long usuario,
			@RequestParam(required = false) String entidad,
			@RequestParam(required = false) String accion,
			@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta) {

		serviciosExternos.verificarAdministrador(autorizacion);

		FiltroAuditoriaDTO filtro = new FiltroAuditoriaDTO(usuario, entidad, accion,
				inicioDelDia(desde), finDelDia(hasta));

		return ResponseEntity.ok(new EventoAuditoriaListaDTO(servicio.consultar(filtro)));
	}

	private static LocalDateTime inicioDelDia(LocalDate fecha) {
		return fecha == null ? null : fecha.atStartOfDay();
	}

	private static LocalDateTime finDelDia(LocalDate fecha) {
		return fecha == null ? null : LocalDateTime.of(fecha, LocalTime.MAX);
	}

}
