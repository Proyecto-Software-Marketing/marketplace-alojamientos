package co.edu.unbosque.marketplace.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.reportes.dto.ReporteTabla;

class GeneradorExcelTest {

    private final GeneradorExcel generador = new GeneradorExcel();

    @Test
    @DisplayName("CA-111 el Excel lleva título, encabezados y filas")
    void generaExcelConLosDatos() throws IOException {
        ReporteTabla tabla = new ReporteTabla("Reporte de usuarios", List.of("Indicador", "Valor"),
                List.of(List.of("Total usuarios", "10"), List.of("Huéspedes", "7")));

        byte[] archivo = generador.generar(tabla);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            XSSFSheet hoja = libro.getSheet("Reporte");
            assertNotNull(hoja);
            assertEquals("Reporte de usuarios", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Indicador", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Valor", hoja.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Total usuarios", hoja.getRow(2).getCell(0).getStringCellValue());
            assertEquals("10", hoja.getRow(2).getCell(1).getStringCellValue());
            assertEquals("Huéspedes", hoja.getRow(3).getCell(0).getStringCellValue());
        }
    }

    @Test
    @DisplayName("CA-112 sin datos el Excel indica que no hay datos")
    void sinDatosEscribeElAviso() throws IOException {
        ReporteTabla tabla = new ReporteTabla("Reporte de demanda", List.of("Ciudad", "Reservas"), List.of());

        byte[] archivo = generador.generar(tabla);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            XSSFSheet hoja = libro.getSheet("Reporte");
            assertNotNull(hoja);
            assertEquals(ReporteTabla.SIN_DATOS, hoja.getRow(2).getCell(0).getStringCellValue());
            assertEquals("", hoja.getRow(2).getCell(1).getStringCellValue());
        }
    }
}
