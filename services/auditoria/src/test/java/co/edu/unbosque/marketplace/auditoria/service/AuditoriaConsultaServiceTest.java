package co.edu.unbosque.marketplace.auditoria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import co.edu.unbosque.marketplace.auditoria.dto.EventoAuditoriaResponseDTO;
import co.edu.unbosque.marketplace.auditoria.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.marketplace.auditoria.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;
import co.edu.unbosque.marketplace.auditoria.repository.EventoAuditoriaRepository;

// HU-41 (CA-114): la consulta de auditoria filtra los eventos que el administrador pide
@SpringBootTest
class AuditoriaConsultaServiceTest {

	@Autowired
	private AuditoriaService servicio;

	@Autowired
	private EventoAuditoriaRepository repositorio;

	@BeforeEach
	void preparar() {
		repositorio.deleteAll();
		repositorio.save(new EventoAuditoria(7L, "REPORTE", null, "EXPORTAR_REPORTE", "EXITO",
				"Reporte: Usuarios; Formato: PDF", LocalDateTime.of(2026, 11, 15, 10, 0)));
		repositorio.save(new EventoAuditoria(9L, "REPORTE", null, "EXPORTAR_REPORTE", "EXITO",
				"Reporte: Reservas; Formato: EXCEL", LocalDateTime.of(2026, 11, 16, 9, 30)));
		repositorio.save(new EventoAuditoria(7L, "RESENA", 42L, "OCULTAR_RESENA", "EXITO",
				"Lenguaje ofensivo", LocalDateTime.of(2026, 11, 16, 11, 0)));
	}

	private List<EventoAuditoriaResponseDTO> consultar(FiltroAuditoriaDTO filtro) {
		return servicio.consultar(filtro);
	}

	@Test
	@DisplayName("CA-114: encuentra las exportaciones por entidad y accion")
	void encuentraLasExportaciones() {
		List<EventoAuditoriaResponseDTO> eventos = consultar(
				new FiltroAuditoriaDTO(null, "reporte", "exportar_reporte", null, null));

		assertEquals(2, eventos.size());
		assertEquals("REPORTE", eventos.get(0).entidad());
		assertEquals("EXPORTAR_REPORTE", eventos.get(0).accion());
	}

	@Test
	@DisplayName("CA-114: filtra por el usuario que exporto")
	void filtraPorUsuario() {
		List<EventoAuditoriaResponseDTO> eventos = consultar(new FiltroAuditoriaDTO(9L, null, null, null, null));

		assertEquals(1, eventos.size());
		assertEquals(9L, eventos.get(0).usuarioId());
	}

	@Test
	@DisplayName("CA-114: filtra por el rango de fechas")
	void filtraPorFechas() {
		List<EventoAuditoriaResponseDTO> eventos = consultar(new FiltroAuditoriaDTO(null, null, null,
				LocalDateTime.of(2026, 11, 16, 0, 0), LocalDateTime.of(2026, 11, 16, 23, 59, 59)));

		assertEquals(2, eventos.size());
		assertEquals("OCULTAR_RESENA", eventos.get(0).accion());
	}

	@Test
	@DisplayName("CA-114: sin filtros llegan todos los eventos del mas reciente al mas antiguo")
	void listaTodosLosEventos() {
		List<EventoAuditoriaResponseDTO> eventos = consultar(new FiltroAuditoriaDTO(null, null, null, null, null));

		assertEquals(3, eventos.size());
		assertEquals("OCULTAR_RESENA", eventos.get(0).accion());
		assertEquals("EXPORTAR_REPORTE", eventos.get(2).accion());
	}

	@Test
	@DisplayName("CA-114: una fecha inicial posterior a la final se rechaza")
	void rechazaRangoInvertido() {
		assertThrows(SolicitudInvalidaException.class, () -> consultar(new FiltroAuditoriaDTO(null, null, null,
				LocalDateTime.of(2026, 11, 20, 0, 0), LocalDateTime.of(2026, 11, 10, 0, 0))));
	}

}
