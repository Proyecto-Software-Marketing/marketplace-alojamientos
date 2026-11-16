package co.edu.unbosque.marketplace.auditoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import co.edu.unbosque.marketplace.auditoria.client.ServiciosExternos;
import co.edu.unbosque.marketplace.auditoria.exception.AutenticacionException;
import co.edu.unbosque.marketplace.auditoria.exception.PermisoException;
import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;
import co.edu.unbosque.marketplace.auditoria.repository.EventoAuditoriaRepository;

// HU-41 (CA-114): el administrador consulta la auditoria y encuentra las exportaciones
@SpringBootTest
class AuditoriaAdminControllerTest {

	private static final String RUTA = "/admin/audit-events";
	private static final String TOKEN = "Bearer token-admin";

	@Autowired
	private WebApplicationContext contexto;

	@Autowired
	private EventoAuditoriaRepository repositorio;

	@MockitoBean
	private ServiciosExternos serviciosExternos;

	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
		repositorio.deleteAll();
	}

	private void guardarEvento(Long usuarioId, String entidad, String accion, LocalDateTime fecha) {
		repositorio.save(new EventoAuditoria(usuarioId, entidad, null, accion, "EXITO",
				"Reporte: Usuarios; Formato: PDF", fecha));
	}

	@Test
	@DisplayName("CA-114: el administrador encuentra el registro de las exportaciones")
	void encuentraLasExportaciones() throws Exception {
		when(serviciosExternos.verificarAdministrador(any())).thenReturn(1L);
		guardarEvento(7L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 15, 10, 0));
		guardarEvento(9L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 16, 9, 30));
		guardarEvento(7L, "RESENA", "OCULTAR_RESENA", LocalDateTime.of(2026, 11, 15, 11, 0));

		mockMvc.perform(get(RUTA)
						.header("Authorization", TOKEN)
						.param("entidad", "REPORTE")
						.param("accion", "EXPORTAR_REPORTE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].usuarioId").value(9))
				.andExpect(jsonPath("$.items[0].entidad").value("REPORTE"))
				.andExpect(jsonPath("$.items[0].accion").value("EXPORTAR_REPORTE"))
				.andExpect(jsonPath("$.items[0].resultado").value("EXITO"))
				.andExpect(jsonPath("$.items[0].detalle").value("Reporte: Usuarios; Formato: PDF"))
				.andExpect(jsonPath("$.items[0].fecha").value("2026-11-16T09:30:00"))
				.andExpect(jsonPath("$.items[1].usuarioId").value(7));
	}

	@Test
	@DisplayName("CA-114: el filtro por usuario deja solo las exportaciones de ese usuario")
	void filtraPorUsuario() throws Exception {
		when(serviciosExternos.verificarAdministrador(any())).thenReturn(1L);
		guardarEvento(7L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 15, 10, 0));
		guardarEvento(9L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 16, 9, 30));

		mockMvc.perform(get(RUTA)
						.header("Authorization", TOKEN)
						.param("usuario", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].usuarioId").value(7));
	}

	@Test
	@DisplayName("CA-114: el filtro por fecha deja solo las exportaciones de ese dia")
	void filtraPorFecha() throws Exception {
		when(serviciosExternos.verificarAdministrador(any())).thenReturn(1L);
		guardarEvento(7L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 15, 10, 0));
		guardarEvento(9L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 16, 9, 30));

		mockMvc.perform(get(RUTA)
						.header("Authorization", TOKEN)
						.param("desde", "2026-11-16")
						.param("hasta", "2026-11-16"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].usuarioId").value(9));
	}

	@Test
	@DisplayName("CA-114: sin filtros llegan todos los eventos, del mas reciente al mas antiguo")
	void listaTodosLosEventos() throws Exception {
		when(serviciosExternos.verificarAdministrador(any())).thenReturn(1L);
		guardarEvento(7L, "REPORTE", "EXPORTAR_REPORTE", LocalDateTime.of(2026, 11, 15, 10, 0));
		guardarEvento(9L, "RESENA", "OCULTAR_RESENA", LocalDateTime.of(2026, 11, 16, 9, 30));

		mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].accion").value("OCULTAR_RESENA"));
	}

	@Test
	@DisplayName("Sin sesion responde 401")
	void sinSesionResponde401() throws Exception {
		when(serviciosExternos.verificarAdministrador(any())).thenThrow(new AutenticacionException("Debe iniciar sesion"));

		mockMvc.perform(get(RUTA))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesion"));
	}

	@Test
	@DisplayName("Si no es administrador responde 403")
	void noAdministradorResponde403() throws Exception {
		when(serviciosExternos.verificarAdministrador(any()))
				.thenThrow(new PermisoException("Solo un administrador puede consultar la auditoria"));

		mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensajes[0]").value("Solo un administrador puede consultar la auditoria"));
	}

	@Test
	@DisplayName("Una fecha que no existe responde 400")
	void fechaInvalidaResponde400() throws Exception {
		when(serviciosExternos.verificarAdministrador(any())).thenReturn(1L);

		mockMvc.perform(get(RUTA)
						.header("Authorization", TOKEN)
						.param("desde", "15-11-2026"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensajes[0]").value("El parametro desde no es valido"));
	}

}
