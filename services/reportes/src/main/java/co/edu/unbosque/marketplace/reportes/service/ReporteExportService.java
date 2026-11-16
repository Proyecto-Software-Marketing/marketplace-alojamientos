package co.edu.unbosque.marketplace.reportes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import co.edu.unbosque.marketplace.reportes.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reportes.dto.ArchivoReporte;
import co.edu.unbosque.marketplace.reportes.dto.CiudadDemanda;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasPublicaciones;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasReservas;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasUsuarios;
import co.edu.unbosque.marketplace.reportes.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.reportes.dto.ExportRequestDTO;
import co.edu.unbosque.marketplace.reportes.dto.FormatoReporte;
import co.edu.unbosque.marketplace.reportes.dto.ReporteTabla;
import co.edu.unbosque.marketplace.reportes.dto.TipoReporte;
import co.edu.unbosque.marketplace.reportes.exception.ServicioNoDisponibleException;

@Service
public class ReporteExportService {

    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String CONTENT_TYPE_EXCEL =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ServiciosExternos serviciosExternos;
    private final GeneradorExcel generadorExcel;
    private final GeneradorPdf generadorPdf;

    public ReporteExportService(ServiciosExternos serviciosExternos, GeneradorExcel generadorExcel,
            GeneradorPdf generadorPdf) {
        this.serviciosExternos = serviciosExternos;
        this.generadorExcel = generadorExcel;
        this.generadorPdf = generadorPdf;
    }

    public ArchivoReporte exportar(String autorizacion, ExportRequestDTO solicitud) {
        Long usuarioId = serviciosExternos.verificarAdministradorParaExportar(autorizacion);
        ReporteTabla tabla = tabla(solicitud.tipoReporte());
        byte[] contenido = solicitud.formato() == FormatoReporte.EXCEL
                ? generadorExcel.generar(tabla)
                : generadorPdf.generar(tabla);
        ArchivoReporte archivo = new ArchivoReporte(contenido, nombreArchivo(solicitud),
                contentType(solicitud.formato()));
        registrarAuditoria(usuarioId, solicitud, archivo);
        return archivo;
    }

    // HU-41 (CA-113): queda registrado quién exportó, qué reporte exportó y cuándo lo exportó
    private void registrarAuditoria(Long usuarioId, ExportRequestDTO solicitud, ArchivoReporte archivo) {
        String detalle = "Reporte: " + nombreReporte(solicitud.tipoReporte())
                + "; Formato: " + solicitud.formato()
                + "; Archivo: " + archivo.nombre();

        serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(
                usuarioId,
                "REPORTE",
                null,
                "EXPORTAR_REPORTE",
                "EXITO",
                detalle));
    }

    private String nombreReporte(TipoReporte tipo) {
        return switch (tipo) {
            case USUARIOS -> "Usuarios";
            case PUBLICACIONES -> "Publicaciones";
            case RESERVAS -> "Reservas";
            case INGRESOS -> "Ingresos estimados";
            case DEMANDA -> "Demanda por ciudad";
        };
    }

    private ReporteTabla tabla(TipoReporte tipo) {
        try {
            return switch (tipo) {
                case USUARIOS -> tablaUsuarios();
                case PUBLICACIONES -> tablaPublicaciones();
                case RESERVAS -> tablaReservas();
                case INGRESOS -> tablaIngresos();
                case DEMANDA -> tablaDemanda();
            };
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible obtener los datos del reporte. Intente más tarde");
        }
    }

    private ReporteTabla tablaUsuarios() {
        EstadisticasUsuarios datos = serviciosExternos.estadisticasUsuarios();
        List<List<String>> filas = new ArrayList<>();
        if (datos.getTotal() > 0 || datos.getHuespedes() > 0 || datos.getAnfitriones() > 0) {
            filas.add(List.of("Total usuarios", String.valueOf(datos.getTotal())));
            filas.add(List.of("Huéspedes", String.valueOf(datos.getHuespedes())));
            filas.add(List.of("Anfitriones", String.valueOf(datos.getAnfitriones())));
        }
        return new ReporteTabla("Reporte de usuarios", List.of("Indicador", "Valor"), filas);
    }

    private ReporteTabla tablaPublicaciones() {
        EstadisticasPublicaciones datos = serviciosExternos.estadisticasPublicaciones();
        List<List<String>> filas = new ArrayList<>();
        if (datos.getActivas() > 0) {
            filas.add(List.of("Publicaciones activas", String.valueOf(datos.getActivas())));
        }
        return new ReporteTabla("Reporte de publicaciones", List.of("Indicador", "Valor"), filas);
    }

    private ReporteTabla tablaReservas() {
        EstadisticasReservas datos = serviciosExternos.estadisticasReservas();
        Map<String, Long> porEstado = datos.getPorEstado() == null ? Map.of() : datos.getPorEstado();
        List<List<String>> filas = porEstado.entrySet().stream()
                .filter(entrada -> entrada.getValue() != null && entrada.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> List.of(entrada.getKey(), String.valueOf(entrada.getValue())))
                .toList();
        return new ReporteTabla("Reporte de reservas", List.of("Estado", "Reservas"), filas);
    }

    private ReporteTabla tablaIngresos() {
        EstadisticasReservas datos = serviciosExternos.estadisticasReservas();
        BigDecimal ingresos = datos.getIngresosEstimados();
        List<List<String>> filas = new ArrayList<>();
        if (ingresos != null && ingresos.signum() > 0) {
            filas.add(List.of("Ingresos estimados", ingresos.toPlainString()));
        }
        return new ReporteTabla("Reporte de ingresos estimados", List.of("Indicador", "Valor"), filas);
    }

    private ReporteTabla tablaDemanda() {
        EstadisticasReservas datos = serviciosExternos.estadisticasReservas();
        List<CiudadDemanda> ciudades = datos.getCiudadesMayorDemanda() == null
                ? List.of()
                : datos.getCiudadesMayorDemanda();
        List<List<String>> filas = ciudades.stream()
                .map(ciudad -> List.of(ciudad.getCiudad(), String.valueOf(ciudad.getReservas())))
                .toList();
        return new ReporteTabla("Reporte de demanda por ciudad", List.of("Ciudad", "Reservas"), filas);
    }

    private String nombreArchivo(ExportRequestDTO solicitud) {
        String extension = solicitud.formato() == FormatoReporte.EXCEL ? "xlsx" : "pdf";
        String tipo = solicitud.tipoReporte().name().toLowerCase(Locale.ROOT);
        return "reporte-" + tipo + "-" + LocalDate.now().format(FORMATO_ARCHIVO) + "." + extension;
    }

    private String contentType(FormatoReporte formato) {
        return formato == FormatoReporte.EXCEL ? CONTENT_TYPE_EXCEL : CONTENT_TYPE_PDF;
    }
}
