package co.edu.unbosque.marketplace.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;
import co.edu.unbosque.marketplace.reportes.exception.ServicioNoDisponibleException;

@ExtendWith(MockitoExtension.class)
class ReporteExportServiceTest {

    private static final String TOKEN_ADMIN = "Bearer token-admin";
    private static final Long ID_ADMIN = 7L;

    @Mock
    private ServiciosExternos serviciosExternos;

    @Mock
    private GeneradorExcel generadorExcel;

    @Mock
    private GeneradorPdf generadorPdf;

    private ReporteExportService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReporteExportService(serviciosExternos, generadorExcel, generadorPdf);
        lenient().when(serviciosExternos.verificarAdministradorParaExportar(anyString())).thenReturn(ID_ADMIN);
    }

    private EstadisticasUsuarios usuarios(long total, long huespedes, long anfitriones) {
        EstadisticasUsuarios usuarios = new EstadisticasUsuarios();
        usuarios.setTotal(total);
        usuarios.setHuespedes(huespedes);
        usuarios.setAnfitriones(anfitriones);
        return usuarios;
    }

    private EstadisticasPublicaciones publicaciones(long activas) {
        EstadisticasPublicaciones publicaciones = new EstadisticasPublicaciones();
        publicaciones.setActivas(activas);
        return publicaciones;
    }

    private CiudadDemanda ciudad(String nombre, long reservas) {
        CiudadDemanda ciudad = new CiudadDemanda();
        ciudad.setCiudad(nombre);
        ciudad.setReservas(reservas);
        return ciudad;
    }

    private EstadisticasReservas reservas(Map<String, Long> porEstado, BigDecimal ingresos,
            List<CiudadDemanda> ciudades) {
        EstadisticasReservas reservas = new EstadisticasReservas();
        reservas.setPorEstado(porEstado);
        reservas.setIngresosEstimados(ingresos);
        reservas.setCiudadesMayorDemanda(ciudades);
        return reservas;
    }

    private ReporteTabla tablaGenerada(TipoReporte tipo, FormatoReporte formato) {
        ArgumentCaptor<ReporteTabla> captor = ArgumentCaptor.forClass(ReporteTabla.class);
        if (formato == FormatoReporte.EXCEL) {
            when(generadorExcel.generar(any())).thenReturn(new byte[] {1, 2, 3});
            servicio.exportar(TOKEN_ADMIN, new ExportRequestDTO(tipo, formato));
            verify(generadorExcel).generar(captor.capture());
        } else {
            when(generadorPdf.generar(any())).thenReturn(new byte[] {1, 2, 3});
            servicio.exportar(TOKEN_ADMIN, new ExportRequestDTO(tipo, formato));
            verify(generadorPdf).generar(captor.capture());
        }
        return captor.getValue();
    }

    @Test
    @DisplayName("CA-111 el reporte de usuarios trae los indicadores de Identidad")
    void reporteDeUsuarios() {
        when(serviciosExternos.estadisticasUsuarios()).thenReturn(usuarios(10, 7, 2));

        ReporteTabla tabla = tablaGenerada(TipoReporte.USUARIOS, FormatoReporte.EXCEL);

        assertEquals("Reporte de usuarios", tabla.titulo());
        assertEquals(List.of("Indicador", "Valor"), tabla.encabezados());
        assertEquals(List.of(
                List.of("Total usuarios", "10"),
                List.of("Huéspedes", "7"),
                List.of("Anfitriones", "2")), tabla.filas());
    }

    @Test
    @DisplayName("CA-111 el reporte de publicaciones trae las activas")
    void reporteDePublicaciones() {
        when(serviciosExternos.estadisticasPublicaciones()).thenReturn(publicaciones(4));

        ReporteTabla tabla = tablaGenerada(TipoReporte.PUBLICACIONES, FormatoReporte.EXCEL);

        assertEquals("Reporte de publicaciones", tabla.titulo());
        assertEquals(List.of(List.of("Publicaciones activas", "4")), tabla.filas());
    }

    @Test
    @DisplayName("CA-111 el reporte de reservas trae los estados con reservas")
    void reporteDeReservas() {
        when(serviciosExternos.estadisticasReservas())
                .thenReturn(reservas(Map.of("CONFIRMADA", 6L, "CANCELADA", 2L, "COMPLETADA", 0L),
                        BigDecimal.ZERO, List.of()));

        ReporteTabla tabla = tablaGenerada(TipoReporte.RESERVAS, FormatoReporte.EXCEL);

        assertEquals("Reporte de reservas", tabla.titulo());
        assertEquals(List.of(
                List.of("CANCELADA", "2"),
                List.of("CONFIRMADA", "6")), tabla.filas());
    }

    @Test
    @DisplayName("CA-111 el reporte de ingresos trae el total estimado")
    void reporteDeIngresos() {
        when(serviciosExternos.estadisticasReservas())
                .thenReturn(reservas(Map.of(), new BigDecimal("2500000"), List.of()));

        ReporteTabla tabla = tablaGenerada(TipoReporte.INGRESOS, FormatoReporte.EXCEL);

        assertEquals("Reporte de ingresos estimados", tabla.titulo());
        assertEquals(List.of(List.of("Ingresos estimados", "2500000")), tabla.filas());
    }

    @Test
    @DisplayName("CA-111 el reporte de demanda trae las ciudades")
    void reporteDeDemanda() {
        when(serviciosExternos.estadisticasReservas())
                .thenReturn(reservas(Map.of(), BigDecimal.ZERO,
                        List.of(ciudad("Bogota", 8), ciudad("Medellin", 5))));

        ReporteTabla tabla = tablaGenerada(TipoReporte.DEMANDA, FormatoReporte.EXCEL);

        assertEquals("Reporte de demanda por ciudad", tabla.titulo());
        assertEquals(List.of(
                List.of("Bogota", "8"),
                List.of("Medellin", "5")), tabla.filas());
    }

    @Test
    @DisplayName("CA-112 sin datos el archivo indica que no hay datos")
    void sinDatosIndicaQueNoHayDatos() {
        when(serviciosExternos.estadisticasUsuarios()).thenReturn(usuarios(0, 0, 0));

        ReporteTabla tabla = tablaGenerada(TipoReporte.USUARIOS, FormatoReporte.PDF);

        assertTrue(tabla.filas().isEmpty());
        assertEquals(List.of(List.of(ReporteTabla.SIN_DATOS, "")), tabla.filasParaMostrar());
    }

    @Test
    @DisplayName("CA-111 el PDF se descarga con su nombre y su tipo de contenido")
    void descargaElPdf() {
        when(serviciosExternos.estadisticasReservas())
                .thenReturn(reservas(Map.of("CONFIRMADA", 6L), BigDecimal.ZERO, List.of()));
        when(generadorPdf.generar(any())).thenReturn(new byte[] {1, 2, 3});

        ArchivoReporte archivo = servicio.exportar(TOKEN_ADMIN,
                new ExportRequestDTO(TipoReporte.RESERVAS, FormatoReporte.PDF));

        assertEquals("application/pdf", archivo.contentType());
        assertTrue(archivo.nombre().startsWith("reporte-reservas-"));
        assertTrue(archivo.nombre().endsWith(".pdf"));
    }

    @Test
    @DisplayName("CA-111 el Excel se descarga con su nombre y su tipo de contenido")
    void descargaElExcel() {
        when(serviciosExternos.estadisticasUsuarios()).thenReturn(usuarios(10, 7, 2));
        when(generadorExcel.generar(any())).thenReturn(new byte[] {1, 2, 3});

        ArchivoReporte archivo = servicio.exportar(TOKEN_ADMIN,
                new ExportRequestDTO(TipoReporte.USUARIOS, FormatoReporte.EXCEL));

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", archivo.contentType());
        assertTrue(archivo.nombre().startsWith("reporte-usuarios-"));
        assertTrue(archivo.nombre().endsWith(".xlsx"));
    }

    @Test
    @DisplayName("Solo el administrador puede exportar")
    void noAdministradorNoExporta() {
        doThrow(new PermisoException("Solo el administrador puede exportar los reportes"))
                .when(serviciosExternos).verificarAdministradorParaExportar(anyString());

        assertThrows(PermisoException.class, () -> servicio.exportar(TOKEN_ADMIN,
                new ExportRequestDTO(TipoReporte.USUARIOS, FormatoReporte.PDF)));

        verify(generadorPdf, never()).generar(any());
        verify(generadorExcel, never()).generar(any());
        verify(serviciosExternos, never()).registrarAuditoria(any());
    }

    @Test
    @DisplayName("CA-113: la exportación queda registrada con quien la pidió, qué reporte fue y el formato")
    void registraLaExportacionEnAuditoria() {
        when(serviciosExternos.estadisticasUsuarios()).thenReturn(usuarios(10, 7, 2));
        when(generadorPdf.generar(any())).thenReturn(new byte[] {1, 2, 3});

        servicio.exportar(TOKEN_ADMIN, new ExportRequestDTO(TipoReporte.USUARIOS, FormatoReporte.PDF));

        ArgumentCaptor<EventoAuditoriaDTO> evento = ArgumentCaptor.forClass(EventoAuditoriaDTO.class);
        verify(serviciosExternos).registrarAuditoria(evento.capture());

        assertEquals(ID_ADMIN, evento.getValue().usuarioId());
        assertEquals("REPORTE", evento.getValue().entidad());
        assertEquals("EXPORTAR_REPORTE", evento.getValue().accion());
        assertEquals("EXITO", evento.getValue().resultado());
        assertTrue(evento.getValue().detalle().contains("Usuarios"));
        assertTrue(evento.getValue().detalle().contains("PDF"));
    }

    @Test
    @DisplayName("CA-113: si la exportación falla no queda registrada")
    void noRegistraSiLaExportacionFalla() {
        when(serviciosExternos.estadisticasUsuarios()).thenThrow(new RestClientException("Sin conexión"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.exportar(TOKEN_ADMIN,
                new ExportRequestDTO(TipoReporte.USUARIOS, FormatoReporte.PDF)));

        verify(serviciosExternos, never()).registrarAuditoria(any());
    }

    @Test
    @DisplayName("Si un servicio externo no responde se avisa con 503")
    void servicioExternoCaido() {
        when(serviciosExternos.estadisticasUsuarios()).thenThrow(new RestClientException("Sin conexión"));

        ServicioNoDisponibleException error = assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.exportar(TOKEN_ADMIN, new ExportRequestDTO(TipoReporte.USUARIOS, FormatoReporte.PDF)));

        assertEquals("No fue posible obtener los datos del reporte. Intente más tarde", error.getMessage());
    }
}
