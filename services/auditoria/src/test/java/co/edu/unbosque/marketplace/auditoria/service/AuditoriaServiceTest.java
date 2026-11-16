package co.edu.unbosque.marketplace.auditoria.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaRequestDTO;
import co.edu.unbosque.marketplace.auditoria.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;
import co.edu.unbosque.marketplace.auditoria.repository.EventoAuditoriaRepository;

@SpringBootTest
class AuditoriaServiceTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 16, 0);
	private static final Long USUARIO = 7L;
	private static final Long ENTIDAD_ID = 42L;

	@Autowired
	private AuditoriaService servicio;

	@Autowired
	private EventoAuditoriaRepository repositorio;

	@MockitoBean
	private Clock reloj;

	@BeforeEach
	void preparar() {
		repositorio.deleteAll();
		when(reloj.getZone()).thenReturn(BOGOTA);
		when(reloj.instant()).thenReturn(AHORA.atZone(BOGOTA).toInstant());
	}

	@Test
	@DisplayName("CA-115: guarda todos los campos del evento con la fecha del servidor")
	void guardaEventoCompleto() {
		EventoAuditoria guardado = servicio.registrar(new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID,
				"OCULTAR_RESENA", "EXITO", "Lenguaje ofensivo"));

		assertNotNull(guardado.getId());
		assertAll(
				() -> assertEquals(USUARIO, guardado.getUsuarioId()),
				() -> assertEquals("RESENA", guardado.getEntidad()),
				() -> assertEquals(ENTIDAD_ID, guardado.getEntidadId()),
				() -> assertEquals("OCULTAR_RESENA", guardado.getAccion()),
				() -> assertEquals("EXITO", guardado.getResultado()),
				() -> assertEquals("Lenguaje ofensivo", guardado.getDetalle()),
				() -> assertEquals(AHORA, guardado.getFecha()),
				() -> assertEquals(1, repositorio.count()));
	}

	@Test
	@DisplayName("CA-115: los campos de texto se guardan sin espacios externos")
	void normalizaCampos() {
		EventoAuditoria guardado = servicio.registrar(new EventoAuditoriaRequestDTO(USUARIO, "  RESENA  ",
				ENTIDAD_ID, "  OCULTAR_RESENA ", " EXITO ", "  motivo  "));

		assertAll(
				() -> assertEquals("RESENA", guardado.getEntidad()),
				() -> assertEquals("OCULTAR_RESENA", guardado.getAccion()),
				() -> assertEquals("EXITO", guardado.getResultado()),
				() -> assertEquals("motivo", guardado.getDetalle()));
	}

	@Test
	@DisplayName("CA-115: un evento del sistema puede no tener usuario, entidad relacionada ni detalle")
	void permiteCamposOpcionales() {
		EventoAuditoria guardado = servicio.registrar(new EventoAuditoriaRequestDTO(null, "SISTEMA", null,
				"INICIAR", "EXITO", null));

		assertNull(guardado.getUsuarioId());
		assertNull(guardado.getEntidadId());
		assertNull(guardado.getDetalle());
	}

	@Test
	@DisplayName("CA-116: el detalle se sanitiza antes de guardarse")
	void sanitizaDetalleAntesDeGuardar() {
		EventoAuditoria guardado = servicio.registrar(new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID,
				"OCULTAR_RESENA", "EXITO", "usuario=7 password=secreto"));

		assertEquals("usuario=7 password=***", guardado.getDetalle());
		assertEquals("usuario=7 password=***", repositorio.findById(guardado.getId()).orElseThrow().getDetalle());
	}

	@Test
	@DisplayName("CA-116: un detalle largo se trunca antes de guardarse")
	void truncaDetalleAntesDeGuardar() {
		EventoAuditoria guardado = servicio.registrar(new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID,
				"OCULTAR_RESENA", "EXITO", "a".repeat(1500)));

		assertEquals(SanitizadorDatosSensibles.LONGITUD_MAXIMA_DETALLE, guardado.getDetalle().length());
	}

	@Test
	@DisplayName("CA-116: un detalle vacío se guarda como nulo")
	void detalleVacioQuedaNulo() {
		EventoAuditoria guardado = servicio.registrar(new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID,
				"OCULTAR_RESENA", "EXITO", "   "));

		assertNull(guardado.getDetalle());
	}

	@Test
	@DisplayName("CA-115: una solicitud nula se rechaza")
	void rechazaSolicitudNula() {
		assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(null));
		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-115: entidad, acción y resultado son obligatorios")
	void rechazaCamposObligatoriosVacios() {
		EventoAuditoriaRequestDTO sinEntidad = new EventoAuditoriaRequestDTO(USUARIO, "  ", ENTIDAD_ID,
				"OCULTAR_RESENA", "EXITO", null);
		EventoAuditoriaRequestDTO sinAccion = new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID, "",
				"EXITO", null);
		EventoAuditoriaRequestDTO sinResultado = new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID,
				"OCULTAR_RESENA", " ", null);

		assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(sinEntidad));
		assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(sinAccion));
		assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(sinResultado));
		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-115: el evento queda en el log estructurado con sus campos")
	void registraLogEstructurado() {
		Logger logger = (Logger) LoggerFactory.getLogger(AuditoriaLogger.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			servicio.registrar(new EventoAuditoriaRequestDTO(USUARIO, "RESENA", ENTIDAD_ID, "OCULTAR_RESENA",
					"EXITO", "usuario=7 password=secreto"));

			ILoggingEvent evento = appender.list.stream()
					.filter(registro -> "Evento de auditoría registrado".equals(registro.getFormattedMessage()))
					.findFirst()
					.orElseThrow();

			Map<String, String> campos = evento.getMDCPropertyMap();
			assertAll(
					() -> assertEquals("7", campos.get("usuarioId")),
					() -> assertEquals("RESENA", campos.get("entidad")),
					() -> assertEquals("42", campos.get("entidadId")),
					() -> assertEquals("OCULTAR_RESENA", campos.get("accion")),
					() -> assertEquals("EXITO", campos.get("resultado")),
					() -> assertEquals("2026-11-15T16:00", campos.get("fecha")),
					() -> assertFalse(campos.get("detalle").contains("secreto")),
					() -> assertTrue(campos.get("detalle").contains("password=***")));
		} finally {
			logger.detachAppender(appender);
			appender.stop();
		}
	}

}
