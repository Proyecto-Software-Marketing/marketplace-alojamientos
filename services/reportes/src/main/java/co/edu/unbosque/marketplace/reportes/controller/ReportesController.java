package co.edu.unbosque.marketplace.reportes.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.reportes.dto.ArchivoReporte;
import co.edu.unbosque.marketplace.reportes.dto.ExportRequestDTO;
import co.edu.unbosque.marketplace.reportes.service.ReporteExportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/reports")
public class ReportesController {

    private final ReporteExportService reporteExportService;

    public ReportesController(ReporteExportService reporteExportService) {
        this.reporteExportService = reporteExportService;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @Valid @RequestBody ExportRequestDTO solicitud) {
        ArchivoReporte archivo = reporteExportService.exportar(autorizacion, solicitud);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.contenido());
    }
}
