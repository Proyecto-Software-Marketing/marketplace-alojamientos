package co.edu.unbosque.marketplace.busqueda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.edu.unbosque.marketplace.busqueda.client.ServiciosExternos;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaResponse;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.exception.BusquedaException;

// Pruebas de los filtros de la busqueda 
@SpringBootTest
class FiltrosTest {

    @Autowired
    private BusquedaService busquedaService;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    @BeforeEach
    void preparar() {
        
        when(serviciosExternos.estaLibre(anyLong(), any(), any())).thenReturn(true);
      
        when(serviciosExternos.buscarPublicacionesActivas("Bogota")).thenReturn(List.of(
                crearPublicacion(1L, "CASA", "100000", List.of("Wi-Fi", "Cocina")),
                crearPublicacion(2L, "APARTAMENTO", "200000", List.of("Wi-Fi")),
                crearPublicacion(3L, "CASA", "300000", null)));
    }

    private PublicacionResumen crearPublicacion(Long id, String tipo, String precio, List<String> servicios) {
        return new PublicacionResumen(id, "Alojamiento " + id, "Bogota", tipo, 4,
                new BigDecimal(precio), "ACTIVA", servicios);
    }


    private BusquedaRequest busquedaBasica() {
        BusquedaRequest datos = new BusquedaRequest();
        datos.setCiudad("Bogota");
        datos.setLlegada(LocalDate.of(2026, 12, 1));
        datos.setSalida(LocalDate.of(2026, 12, 5));
        datos.setHuespedes(2);
        return datos;
    }

    private List<Long> ids(BusquedaResponse respuesta) {
        return respuesta.getResultados().stream().map(PublicacionResumen::getId).toList();
    }

    @Test
    void sinFiltrosSalenTodas() {
        BusquedaRequest datos = busquedaBasica();
        datos.setTipo("");

        assertEquals(List.of(1L, 2L, 3L), ids(busquedaService.buscar(datos)));
    }

    // CA-056
    @Test
    void filtraPorTipo() {
        BusquedaRequest datos = busquedaBasica();
        datos.setTipo("casa");

        assertEquals(List.of(1L, 3L), ids(busquedaService.buscar(datos)));
    }

    // CA-056
    @Test
    void filtraPorRangoDePrecio() {
        BusquedaRequest datos = busquedaBasica();
        datos.setPrecioMinimo(new BigDecimal("150000"));
        datos.setPrecioMaximo(new BigDecimal("250000"));

        assertEquals(List.of(2L), ids(busquedaService.buscar(datos)));
    }

    // CA-056
    @Test
    void filtraSoloConPrecioMinimo() {
        BusquedaRequest datos = busquedaBasica();
        datos.setPrecioMinimo(new BigDecimal("200000"));

        assertEquals(List.of(2L, 3L), ids(busquedaService.buscar(datos)));
    }

    // CA-056
    @Test
    void filtraPorServicios() {
        BusquedaRequest datos = busquedaBasica();
        datos.setServicios(List.of("Wi-Fi", "Cocina"));

        assertEquals(List.of(1L), ids(busquedaService.buscar(datos)));
    }

    // CA-056
    @Test
    void serviciosVaciosNoFiltran() {
        BusquedaRequest datos = busquedaBasica();
        datos.setServicios(List.of());

        assertEquals(List.of(1L, 2L, 3L), ids(busquedaService.buscar(datos)));
    }

    // CA-056
    @Test
    void cumpleTodosLosFiltrosALaVez() {
        BusquedaRequest datos = busquedaBasica();
        datos.setTipo("CASA");
        datos.setPrecioMaximo(new BigDecimal("150000"));
        datos.setServicios(List.of("Wi-Fi"));

        BusquedaResponse respuesta = busquedaService.buscar(datos);

        assertEquals(List.of(1L), ids(respuesta));
        assertNull(respuesta.getMensaje());
    }

    // CA-057
    @Test
    void sinResultadosMuestraMensaje() {
        BusquedaRequest datos = busquedaBasica();
        datos.setTipo("CABAÑA");

        BusquedaResponse respuesta = busquedaService.buscar(datos);

        assertTrue(respuesta.getResultados().isEmpty());
        assertEquals("No hay alojamientos que cumplan con la búsqueda", respuesta.getMensaje());
    }

    @Test
    void precioMinimoMayorQueMaximoDaError() {
        BusquedaRequest datos = busquedaBasica();
        datos.setPrecioMinimo(new BigDecimal("300000"));
        datos.setPrecioMaximo(new BigDecimal("100000"));

        BusquedaException error = assertThrows(BusquedaException.class, () -> busquedaService.buscar(datos));
        assertEquals("El precio mínimo no puede ser mayor que el precio máximo", error.getMessage());
    }
}