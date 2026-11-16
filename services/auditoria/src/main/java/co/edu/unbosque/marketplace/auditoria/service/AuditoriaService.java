package co.edu.unbosque.marketplace.auditoria.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaRequestDTO;
import co.edu.unbosque.marketplace.auditoria.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;
import co.edu.unbosque.marketplace.auditoria.repository.EventoAuditoriaRepository;

@Service
public class AuditoriaService {

	private final EventoAuditoriaRepository repositorio;
	private final SanitizadorDatosSensibles sanitizador;
	private final AuditoriaLogger logger;
	private final Clock reloj;

	public AuditoriaService(EventoAuditoriaRepository repositorio, SanitizadorDatosSensibles sanitizador,
			AuditoriaLogger logger, Clock reloj) {
		this.repositorio = repositorio;
		this.sanitizador = sanitizador;
		this.logger = logger;
		this.reloj = reloj;
	}

	@Transactional
	public EventoAuditoria registrar(EventoAuditoriaRequestDTO solicitud) {
		if (solicitud == null) {
			throw new SolicitudInvalidaException("La solicitud es obligatoria");
		}

		String entidad = normalizar(solicitud.entidad());
		String accion = normalizar(solicitud.accion());
		String resultado = normalizar(solicitud.resultado());

		if (entidad == null) {
			throw new SolicitudInvalidaException("La entidad es obligatoria");
		}
		if (accion == null) {
			throw new SolicitudInvalidaException("La acción es obligatoria");
		}
		if (resultado == null) {
			throw new SolicitudInvalidaException("El resultado es obligatorio");
		}

		EventoAuditoria evento = new EventoAuditoria(solicitud.usuarioId(), entidad, solicitud.entidadId(), accion,
				resultado, sanitizador.limpiar(solicitud.detalle()), LocalDateTime.now(reloj));

		EventoAuditoria guardado = repositorio.save(evento);
		logger.evento(guardado);
		return guardado;
	}

	private static String normalizar(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = valor.trim();
		return limpio.isEmpty() ? null : limpio;
	}

}
