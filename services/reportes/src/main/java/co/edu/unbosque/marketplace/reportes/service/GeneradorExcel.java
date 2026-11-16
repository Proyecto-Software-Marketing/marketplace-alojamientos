package co.edu.unbosque.marketplace.reportes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.reportes.dto.ReporteTabla;

@Component
public class GeneradorExcel {

    private static final String NOMBRE_HOJA = "Reporte";
    private static final int ANCHO_COLUMNA = 24;

    public byte[] generar(ReporteTabla tabla) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            XSSFSheet hoja = libro.createSheet(NOMBRE_HOJA);
            CellStyle estiloNegrita = estiloNegrita(libro);

            Row filaTitulo = hoja.createRow(0);
            filaTitulo.createCell(0).setCellValue(tabla.titulo());
            filaTitulo.getCell(0).setCellStyle(estiloNegrita);

            Row filaEncabezado = hoja.createRow(1);
            List<String> encabezados = tabla.encabezados();
            for (int columna = 0; columna < encabezados.size(); columna++) {
                filaEncabezado.createCell(columna).setCellValue(encabezados.get(columna));
                filaEncabezado.getCell(columna).setCellStyle(estiloNegrita);
            }

            int siguienteFila = 2;
            for (List<String> fila : tabla.filasParaMostrar()) {
                Row filaDatos = hoja.createRow(siguienteFila++);
                for (int columna = 0; columna < fila.size(); columna++) {
                    filaDatos.createCell(columna).setCellValue(fila.get(columna));
                }
            }

            if (encabezados.size() > 1) {
                hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, encabezados.size() - 1));
            }
            for (int columna = 0; columna < encabezados.size(); columna++) {
                hoja.setColumnWidth(columna, ANCHO_COLUMNA * 256);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible generar el archivo Excel", e);
        }
    }

    private static CellStyle estiloNegrita(XSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }
}
