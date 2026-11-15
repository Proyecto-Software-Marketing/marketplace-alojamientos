package co.edu.unbosque.marketplace.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.edu.unbosque.marketplace.reportes.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reportes.controller.DashboardController;
import co.edu.unbosque.marketplace.reportes.dto.CiudadDemanda;
import co.edu.unbosque.marketplace.reportes.dto.DashboardResponse;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasPublicaciones;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasReservas;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasUsuarios;
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;

// Pruebas del dashboard del administrador 
@SpringBootTest
class DashboardTest {

    private static final String TOKEN_ADMIN = "Bearer token-admin";
    private static final String TOKEN_HUESPED = "Bearer token-huesped";

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DashboardController dashboardController;

    // simulamos Identidad, Alojamientos y Reservas
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private EstadisticasUsuarios usuarios(long total) {
        EstadisticasUsuarios usuarios = new EstadisticasUsuarios();
        usuarios.setTotal(total);
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

    private EstadisticasReservas reservas() {
        EstadisticasReservas reservas = new EstadisticasReservas();
        reservas.setPorEstado(Map.of("CONFIRMADA", 6L, "CANCELADA", 2L, "COMPLETADA", 5L));
        reservas.setIngresosEstimados(new BigDecimal("2500000"));
        reservas.setCiudadesMayorDemanda(List.of(ciudad("Bogota", 8), ciudad("Medellin", 5)));
        return reservas;
    }

    @BeforeEach
    void preparar() {
    
        doThrow(new PermisoException("Solo el administrador puede ver el dashboard"))
                .when(serviciosExternos).verificarAdministrador(TOKEN_HUESPED);
        when(serviciosExternos.estadisticasUsuarios()).thenReturn(usuarios(10));
        when(serviciosExternos.estadisticasPublicaciones()).thenReturn(publicaciones(4));
        when(serviciosExternos.estadisticasReservas()).thenReturn(reservas());
    }

    @Test
    @DisplayName("CA-108 el administrador ve todos los indicadores")
    void muestraTodosLosIndicadores() {
        DashboardResponse dashboard = dashboardService.consultar(TOKEN_ADMIN);

        assertEquals(10, dashboard.getUsuarios());
        assertEquals(4, dashboard.getPublicacionesActivas());
        assertEquals(6L, dashboard.getReservasPorEstado().get("CONFIRMADA"));
        assertEquals(2L, dashboard.getReservasPorEstado().get("CANCELADA"));
        assertEquals(0, new BigDecimal("2500000").compareTo(dashboard.getIngresosEstimados()));
        assertEquals("Bogota", dashboard.getCiudadesMayorDemanda().get(0).getCiudad());
        assertEquals(8, dashboard.getCiudadesMayorDemanda().get(0).getReservas());
    }

    @Test
    @DisplayName("CA-109 al recargar, los indicadores se actualizan")
    void losIndicadoresSeActualizan() {
        when(serviciosExternos.estadisticasUsuarios()).thenReturn(usuarios(10), usuarios(11));

        assertEquals(10, dashboardService.consultar(TOKEN_ADMIN).getUsuarios());
        assertEquals(11, dashboardService.consultar(TOKEN_ADMIN).getUsuarios());
    }

    @Test
    @DisplayName("CA-110 si no es administrador se le niega el acceso")
    void siNoEsAdministradorNoEntra() {
        PermisoException error = assertThrows(PermisoException.class,
                () -> dashboardService.consultar(TOKEN_HUESPED));

        assertEquals("Solo el administrador puede ver el dashboard", error.getMessage());
      
        verify(serviciosExternos, never()).estadisticasUsuarios();
    }

    @Test
    @DisplayName("Si Alojamientos y Reservas no responden datos, quedan en cero")
    void sinDatosQuedaEnCero() {
        when(serviciosExternos.estadisticasPublicaciones()).thenReturn(new EstadisticasPublicaciones());
        when(serviciosExternos.estadisticasReservas()).thenReturn(new EstadisticasReservas());

        DashboardResponse dashboard = dashboardService.consultar(TOKEN_ADMIN);

        assertEquals(0, dashboard.getPublicacionesActivas());
        assertTrue(dashboard.getReservasPorEstado().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(dashboard.getIngresosEstimados()));
        assertTrue(dashboard.getCiudadesMayorDemanda().isEmpty());
    }

    @Test
    void elControllerDevuelveElDashboard() {
        assertEquals(10, dashboardController.dashboard(TOKEN_ADMIN).getUsuarios());
    }
}