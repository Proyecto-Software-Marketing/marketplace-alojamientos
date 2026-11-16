package co.edu.unbosque.marketplace.auditoria.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;

@Component
public class AuditoriaLogger {

	private static final Logger LOG = LoggerFactory.getLogger(AuditoriaLogger.class);

	public void evento(EventoAuditoria evento) {
		Map<String, String> campos = new LinkedHashMap<>();
		campos.put("usuarioId", texto(evento.getUsuarioId()));
		campos.put("entidad", evento.getEntidad());
		campos.put("entidadId", texto(evento.getEntidadId()));
		campos.put("accion", evento.getAccion());
		campos.put("resultado", evento.getResultado());
		campos.put("detalle", evento.getDetalle());
		campos.put("fecha", texto(evento.getFecha()));

		registrar("Evento de auditoría registrado", campos, false);
	}

	public void error(String accion, String detalle, String error, String ruta, String metodo, int estado,
			LocalDateTime fecha) {
		Map<String, String> campos = new LinkedHashMap<>();
		campos.put("entidad", "ERROR");
		campos.put("accion", accion);
		campos.put("resultado", "ERROR");
		campos.put("detalle", detalle);
		campos.put("fecha", texto(fecha));
		campos.put("error", error);
		campos.put("ruta", ruta);
		campos.put("metodo", metodo);
		campos.put("estado", String.valueOf(estado));

		registrar("Error procesando una solicitud de auditoría", campos, true);
	}

	private void registrar(String mensaje, Map<String, String> campos, boolean error) {
		List<String> claves = new ArrayList<>();

		for (Map.Entry<String, String> campo : campos.entrySet()) {
			if (campo.getValue() != null && !campo.getValue().isBlank()) {
				MDC.put(campo.getKey(), campo.getValue());
				claves.add(campo.getKey());
			}
		}

		try {
			if (error) {
				LOG.error(mensaje);
			} else {
				LOG.info(mensaje);
			}
		} finally {
			claves.forEach(MDC::remove);
		}
	}

	private static String texto(Object valor) {
		return valor == null ? null : valor.toString();
	}

}
