package co.edu.unbosque.marketplace.reportes.dto;

import java.util.List;
import java.util.stream.IntStream;

public record ReporteTabla(String titulo, List<String> encabezados, List<List<String>> filas) {

    public static final String SIN_DATOS = "No hay datos disponibles para este reporte";

    public ReporteTabla {
        encabezados = List.copyOf(encabezados);
        filas = filas.stream().map(List::copyOf).toList();
    }

    public List<List<String>> filasParaMostrar() {
        if (!filas.isEmpty()) {
            return filas;
        }
        return List.of(IntStream.range(0, encabezados.size())
                .mapToObj(columna -> columna == 0 ? SIN_DATOS : "")
                .toList());
    }
}
