package co.edu.unbosque.marketplace.auditoria.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.unbosque.marketplace.auditoria.service.AuditoriaLogger;

class ControlErroresTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 16, 0);

	private final Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private final AuditoriaLogger logger = new AuditoriaLogger();
	private final ControlErrores controlErrores = new ControlErrores(logger, reloj);

	private MockHttpServletRequest solicitud() {
		MockHttpServletRequest solicitud = new MockHttpServletRequest();
		solicitud.setRequestURI("/internal/audit-events");
		solicitud.setMethod("POST");
		return solicitud;
	}

	@Test
	@DisplayName("CA-115: una regla del servicio responde 400 con su mensaje")
	void solicitudInvalida() {
		ResponseEntity<Map<String, List<String>>> respuesta = controlErrores
				.solicitudInvalida(new SolicitudInvalidaException("La entidad es obligatoria"), solicitud());

		assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
		assertEquals(List.of("La entidad es obligatoria"), respuesta.getBody().get("mensajes"));
	}

	@Test
	@DisplayName("CA-116: un error inesperado responde 500 sin exponer el mensaje interno")
	void errorInterno() {
		Logger loggerLogback = (Logger) LoggerFactory.getLogger(AuditoriaLogger.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		loggerLogback.addAppender(appender);

		try {
			ResponseEntity<Map<String, List<String>>> respuesta = controlErrores
					.errorInterno(new RuntimeException("password=secreto"), solicitud());

			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
			assertEquals(List.of("No fue posible procesar la solicitud"),
					respuesta.getBody().get("mensajes"));

			ILoggingEvent registro = appender.list.stream()
					.filter(evento -> "Error procesando una solicitud de auditoría"
							.equals(evento.getFormattedMessage()))
					.findFirst()
					.orElseThrow();

			assertEquals(Level.ERROR, registro.getLevel());
			Map<String, String> campos = registro.getMDCPropertyMap();
			assertEquals("ERROR_INTERNO", campos.get("accion"));
			assertEquals("RuntimeException", campos.get("error"));
			assertEquals("500", campos.get("estado"));
			assertEquals("Error interno al procesar la solicitud", campos.get("detalle"));
			assertFalse(campos.values().stream().anyMatch(valor -> valor.contains("secreto")));
			assertFalse(registro.getFormattedMessage().contains("secreto"));
		} finally {
			loggerLogback.detachAppender(appender);
			appender.stop();
		}
	}

}
