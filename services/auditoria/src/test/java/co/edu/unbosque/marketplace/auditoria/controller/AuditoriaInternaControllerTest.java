package co.edu.unbosque.marketplace.auditoria.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.unbosque.marketplace.auditoria.repository.EventoAuditoriaRepository;
import co.edu.unbosque.marketplace.auditoria.service.AuditoriaLogger;

@SpringBootTest
class AuditoriaInternaControllerTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 16, 0);

	@Autowired
	private WebApplicationContext contexto;

	@Autowired
	private EventoAuditoriaRepository repositorio;

	@MockitoBean
	private Clock reloj;

	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
		repositorio.deleteAll();
		when(reloj.getZone()).thenReturn(BOGOTA);
		when(reloj.instant()).thenReturn(AHORA.atZone(BOGOTA).toInstant());
	}

	private ResultActions registrar(String contenido) throws Exception {
		return mockMvc.perform(post("/internal/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido));
	}

	@Test
	@DisplayName("CA-115: registra un evento completo y responde 201")
	void registraEvento() throws Exception {
		registrar("{\"usuarioId\":7,\"entidad\":\"RESENA\",\"entidadId\":42,"
				+ "\"accion\":\"OCULTAR_RESENA\",\"resultado\":\"EXITO\",\"detalle\":\"Lenguaje ofensivo\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.usuarioId").value(7))
				.andExpect(jsonPath("$.entidad").value("RESENA"))
				.andExpect(jsonPath("$.entidadId").value(42))
				.andExpect(jsonPath("$.accion").value("OCULTAR_RESENA"))
				.andExpect(jsonPath("$.resultado").value("EXITO"))
				.andExpect(jsonPath("$.detalle").value("Lenguaje ofensivo"))
				.andExpect(jsonPath("$.fecha").value(startsWith("2026-11-15T16:00")));

		assertEquals(1, repositorio.count());
	}

	@Test
	@DisplayName("CA-115: registra un evento sin usuario ni entidad relacionada")
	void registraEventoSinOpcionales() throws Exception {
		registrar("{\"entidad\":\"SISTEMA\",\"accion\":\"INICIAR\",\"resultado\":\"EXITO\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.usuarioId").value(nullValue()))
				.andExpect(jsonPath("$.entidadId").value(nullValue()))
				.andExpect(jsonPath("$.detalle").value(nullValue()));

		assertEquals(1, repositorio.count());
	}

	@Test
	@DisplayName("CA-116: el detalle con credenciales se responde y se guarda sanitizado")
	void sanitizaDetalle() throws Exception {
		registrar("{\"usuarioId\":7,\"entidad\":\"RESENA\",\"entidadId\":42,"
				+ "\"accion\":\"OCULTAR_RESENA\",\"resultado\":\"EXITO\","
				+ "\"detalle\":\"Authorization: Bearer abc.def, password=secreto\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.detalle").value("Authorization: ***, password=***"));

		assertEquals("Authorization: ***, password=***",
				repositorio.findAll().getFirst().getDetalle());
	}

	@Test
	@DisplayName("CA-115: sin entidad, acción ni resultado se responde 400")
	void camposObligatorios() throws Exception {
		registrar("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes.length()").value(3))
				.andExpect(jsonPath("$.mensajes[0]").value("El resultado es obligatorio"))
				.andExpect(jsonPath("$.mensajes[1]").value("La acción es obligatoria"))
				.andExpect(jsonPath("$.mensajes[2]").value("La entidad es obligatoria"));

		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-115: con campos obligatorios en blanco se responde 400")
	void camposEnBlanco() throws Exception {
		registrar("{\"entidad\":\"  \",\"accion\":\"\",\"resultado\":\" \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes.length()").value(3));

		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-115: los identificadores deben ser positivos")
	void identificadoresInvalidos() throws Exception {
		registrar("{\"usuarioId\":0,\"entidad\":\"RESENA\",\"entidadId\":-1,"
				+ "\"accion\":\"CONSULTAR\",\"resultado\":\"EXITO\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes.length()").value(2))
				.andExpect(jsonPath("$.mensajes[0]").value("El usuario debe ser válido"))
				.andExpect(jsonPath("$.mensajes[1]").value("La entidad relacionada debe ser válida"));

		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-115: la entidad y la acción tienen longitud máxima")
	void camposDemasiadoLargos() throws Exception {
		registrar("{\"entidad\":\"" + "a".repeat(41) + "\",\"accion\":\"CONSULTAR\",\"resultado\":\"EXITO\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes[0]").value("La entidad no puede superar 40 caracteres"));

		registrar("{\"entidad\":\"RESENA\",\"accion\":\"" + "a".repeat(61) + "\",\"resultado\":\"EXITO\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes[0]").value("La acción no puede superar 60 caracteres"));
	}

	@Test
	@DisplayName("CA-116: un detalle mayor a 2000 caracteres se rechaza antes de procesarlo")
	void detalleDemasiadoLargo() throws Exception {
		registrar("{\"entidad\":\"RESENA\",\"accion\":\"CONSULTAR\",\"resultado\":\"EXITO\","
				+ "\"detalle\":\"" + "a".repeat(2001) + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes[0]").value("El detalle no puede superar 2000 caracteres"));

		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-116: un JSON inválido responde 400 con un mensaje genérico")
	void jsonInvalido() throws Exception {
		registrar("{\"usuarioId\":\"abc\",\"entidad\":\"RESENA\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));

		assertEquals(0, repositorio.count());
	}

	@Test
	@DisplayName("CA-116: un error de deserialización se loguea sin el cuerpo sensible")
	void errorDeDeserializacionNoExponeSecretos() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(AuditoriaLogger.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			registrar("{\"usuarioId\":\"abc\",\"entidad\":\"RESENA\",\"password\":\"secreto\"}")
					.andExpect(status().isBadRequest());

			ILoggingEvent registro = appender.list.stream()
					.filter(evento -> "Error procesando una solicitud de auditoría"
							.equals(evento.getFormattedMessage()))
					.findFirst()
					.orElseThrow();

			Map<String, String> campos = registro.getMDCPropertyMap();
			assertEquals("ERROR", campos.get("entidad"));
			assertEquals("ERROR", campos.get("resultado"));
			assertEquals("DESERIALIZACION", campos.get("accion"));
			assertEquals("HttpMessageNotReadableException", campos.get("error"));
			assertEquals("/internal/audit-events", campos.get("ruta"));
			assertEquals("POST", campos.get("metodo"));
			assertEquals("400", campos.get("estado"));
			assertFalse(campos.values().stream().anyMatch(valor -> valor.contains("secreto")));
			assertFalse(registro.getFormattedMessage().contains("secreto"));
			assertTrue(campos.get("detalle").contains("formato inválido"));
		} finally {
			logger.detachAppender(appender);
			appender.stop();
		}
	}

}
