package co.edu.unbosque.marketplace.reportes.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import co.edu.unbosque.marketplace.reportes.dto.ArchivoReporte;
import co.edu.unbosque.marketplace.reportes.dto.ExportRequestDTO;
import co.edu.unbosque.marketplace.reportes.dto.FormatoReporte;
import co.edu.unbosque.marketplace.reportes.dto.TipoReporte;
import co.edu.unbosque.marketplace.reportes.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;
import co.edu.unbosque.marketplace.reportes.service.ReporteExportService;

@SpringBootTest
class ReportesControllerTest {

    private static final String RUTA = "/admin/reports/export";
    private static final String TOKEN = "Bearer token-admin";

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private ReporteExportService reporteExportService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    @DisplayName("CA-111 descarga el reporte en PDF")
    void descargaElPdf() throws Exception {
        when(reporteExportService.exportar(eq(TOKEN), any()))
                .thenReturn(new ArchivoReporte("%PDF".getBytes(StandardCharsets.US_ASCII),
                        "reporte-usuarios-2026-01-01.pdf", "application/pdf"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoReporte\":\"USUARIOS\",\"formato\":\"PDF\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-usuarios-2026-01-01.pdf\""));

        ArgumentCaptor<ExportRequestDTO> solicitud = ArgumentCaptor.forClass(ExportRequestDTO.class);
        verify(reporteExportService).exportar(eq(TOKEN), solicitud.capture());
        assertEquals(TipoReporte.USUARIOS, solicitud.getValue().tipoReporte());
        assertEquals(FormatoReporte.PDF, solicitud.getValue().formato());
    }

    @Test
    @DisplayName("CA-111 descarga el reporte en Excel")
    void descargaElExcel() throws Exception {
        when(reporteExportService.exportar(eq(TOKEN), any()))
                .thenReturn(new ArchivoReporte(new byte[] {1, 2, 3}, "reporte-reservas-2026-01-01.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoReporte\":\"RESERVAS\",\"formato\":\"EXCEL\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-reservas-2026-01-01.xlsx\""));
    }

    @Test
    @DisplayName("Sin sesión responde 401")
    void sinSesionResponde401() throws Exception {
        when(reporteExportService.exportar(isNull(), any()))
                .thenThrow(new AutenticacionException("Debe iniciar sesión"));

        mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoReporte\":\"USUARIOS\",\"formato\":\"PDF\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("Si no es administrador responde 403")
    void noAdministradorResponde403() throws Exception {
        when(reporteExportService.exportar(eq(TOKEN), any()))
                .thenThrow(new PermisoException("Solo el administrador puede ver el dashboard"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoReporte\":\"USUARIOS\",\"formato\":\"PDF\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo el administrador puede ver el dashboard"));
    }

    @Test
    @DisplayName("Un tipo de reporte que no existe responde 400")
    void tipoDeReporteInvalidoResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoReporte\":\"INVENTADO\",\"formato\":\"PDF\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El tipo de reporte o el formato no son válidos"));

        verify(reporteExportService, never()).exportar(any(), any());
    }

    @Test
    @DisplayName("Si falta el formato responde 400")
    void sinFormatoResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoReporte\":\"USUARIOS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El formato del archivo es obligatorio"));

        verify(reporteExportService, never()).exportar(any(), any());
    }
}
