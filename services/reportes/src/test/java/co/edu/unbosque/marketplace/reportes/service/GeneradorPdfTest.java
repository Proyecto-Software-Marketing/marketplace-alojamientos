package co.edu.unbosque.marketplace.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.reportes.dto.ReporteTabla;

class GeneradorPdfTest {

    private final GeneradorPdf generador = new GeneradorPdf();

    @Test
    @DisplayName("CA-111 el PDF se genera con los datos del reporte")
    void generaPdfValido() {
        ReporteTabla tabla = new ReporteTabla("Reporte de reservas", List.of("Estado", "Reservas"),
                List.of(List.of("CONFIRMADA", "6")));

        byte[] archivo = generador.generar(tabla);

        assertTrue(archivo.length > 0);
        assertEquals("%PDF", new String(archivo, 0, 4, StandardCharsets.US_ASCII));
        assertTrue(ultimosBytes(archivo, 32).contains("%%EOF"));
    }

    @Test
    @DisplayName("CA-112 sin datos el PDF también se genera")
    void sinDatosGeneraPdf() {
        ReporteTabla tabla = new ReporteTabla("Reporte de ingresos estimados", List.of("Indicador", "Valor"), List.of());

        byte[] archivo = generador.generar(tabla);

        assertTrue(archivo.length > 0);
        assertEquals("%PDF", new String(archivo, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("El PDF conserva los acentos de los encabezados y los datos")
    void conservaLosAcentos() {
        ReporteTabla tabla = new ReporteTabla("Reporte de usuarios", List.of("Indicador", "Valor"),
                List.of(List.of("Huéspedes", "7"), List.of("No hay datos disponibles para este reporte", "")));

        byte[] archivo = generador.generar(tabla, false);

        String contenido = new String(archivo, StandardCharsets.ISO_8859_1);
        assertTrue(contenido.contains("Huéspedes"));
        assertTrue(contenido.contains("No hay datos disponibles para este reporte"));
    }

    private static String ultimosBytes(byte[] archivo, int cantidad) {
        int inicio = Math.max(0, archivo.length - cantidad);
        return new String(archivo, inicio, archivo.length - inicio, StandardCharsets.US_ASCII);
    }
}
