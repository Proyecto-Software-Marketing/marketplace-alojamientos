package co.edu.unbosque.marketplace.auditoria.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaRequestDTO;
import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaResponseDTO;
import co.edu.unbosque.marketplace.auditoria.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.marketplace.auditoria.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;
import co.edu.unbosque.marketplace.auditoria.repository.EventoAuditoriaRepository;

@Service
public class AuditoriaService {

	private static final int LIMITE_EVENTOS = 200;

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

	// HU-41 (CA-114): el administrador consulta la auditoria y encuentra, entre otros, los exports
	@Transactional(readOnly = true)
	public List<EventoAuditoriaResponseDTO> consultar(FiltroAuditoriaDTO filtro) {
		if (filtro == null) {
			throw new SolicitudInvalidaException("Los filtros son obligatorios");
		}
		if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
			throw new SolicitudInvalidaException("La fecha inicial no puede ser posterior a la fecha final");
		}

		List<EventoAuditoria> eventos = repositorio
				.findAll(especificacion(filtro), PageRequest.of(0, LIMITE_EVENTOS,
						Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id"))))
				.getContent();

		return eventos.stream().map(EventoAuditoriaResponseDTO::desde).toList();
	}

	private static Specification<EventoAuditoria> especificacion(FiltroAuditoriaDTO filtro) {
		Specification<EventoAuditoria> filtros = (raiz, consulta, criterios) -> criterios.conjunction();

		if (filtro.usuario() != null) {
			filtros = filtros.and((raiz, consulta, criterios) -> criterios.equal(raiz.get("usuarioId"),
					filtro.usuario()));
		}

		String entidad = normalizar(filtro.entidad());
		if (entidad != null) {
			filtros = filtros.and((raiz, consulta, criterios) -> criterios.equal(raiz.get("entidad"),
					entidad.toUpperCase()));
		}

		String accion = normalizar(filtro.accion());
		if (accion != null) {
			filtros = filtros.and((raiz, consulta, criterios) -> criterios.equal(raiz.get("accion"),
					accion.toUpperCase()));
		}

		if (filtro.desde() != null) {
			filtros = filtros.and((raiz, consulta, criterios) -> criterios.greaterThanOrEqualTo(raiz.get("fecha"),
					filtro.desde()));
		}

		if (filtro.hasta() != null) {
			filtros = filtros.and((raiz, consulta, criterios) -> criterios.lessThanOrEqualTo(raiz.get("fecha"),
					filtro.hasta()));
		}

		return filtros;
	}

	private static String normalizar(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = valor.trim();
		return limpio.isEmpty() ? null : limpio;
	}

}
