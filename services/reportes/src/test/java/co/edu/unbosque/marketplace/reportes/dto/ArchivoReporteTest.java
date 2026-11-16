package co.edu.unbosque.marketplace.reportes.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchivoReporteTest {

    @Test
    @DisplayName("CA-111 dos archivos con el mismo contenido son iguales")
    void comparaPorContenido() {
        ArchivoReporte primero = new ArchivoReporte("PDF".getBytes(StandardCharsets.US_ASCII),
                "reporte-usuarios-2026-01-01.pdf", "application/pdf");
        ArchivoReporte segundo = new ArchivoReporte("PDF".getBytes(StandardCharsets.US_ASCII),
                "reporte-usuarios-2026-01-01.pdf", "application/pdf");

        assertEquals(primero, segundo);
        assertEquals(primero.hashCode(), segundo.hashCode());
    }

    @Test
    @DisplayName("CA-111 dos archivos con distinto contenido no son iguales")
    void diferenciaPorContenido() {
        ArchivoReporte primero = new ArchivoReporte("PDF".getBytes(StandardCharsets.US_ASCII),
                "reporte-usuarios-2026-01-01.pdf", "application/pdf");
        ArchivoReporte segundo = new ArchivoReporte("XLSX".getBytes(StandardCharsets.US_ASCII),
                "reporte-usuarios-2026-01-01.pdf", "application/pdf");

        assertNotEquals(primero, segundo);
    }

    @Test
    @DisplayName("CA-111 el toString muestra el nombre y el contenido")
    void muestraElContenido() {
        ArchivoReporte archivo = new ArchivoReporte("PDF".getBytes(StandardCharsets.US_ASCII),
                "reporte-usuarios-2026-01-01.pdf", "application/pdf");

        String texto = archivo.toString();

        assertTrue(texto.contains("reporte-usuarios-2026-01-01.pdf"));
        assertTrue(texto.contains("application/pdf"));
        assertTrue(texto.contains("[80, 68, 70]"));
    }
}
