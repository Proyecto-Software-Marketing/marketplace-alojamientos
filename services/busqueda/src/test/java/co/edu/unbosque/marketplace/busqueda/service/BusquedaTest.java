package co.edu.unbosque.marketplace.busqueda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import co.edu.unbosque.marketplace.busqueda.controller.BusquedaController;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaResponse;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.exception.BusquedaException;
import jakarta.validation.Validator;

@SpringBootTest
class BusquedaTest {

    @Autowired
    private BusquedaService busquedaService;

    @Autowired
    private BusquedaController busquedaController;

    @Autowired
    private Validator validator;

    // simulacion de Alojamientos y Disponibilidad
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private final LocalDate llegada = LocalDate.of(2026, 12, 1);
    private final LocalDate salida = LocalDate.of(2026, 12, 5);

    @BeforeEach
    void preparar() {
        // por defecto todas las publicaciones estan libres
        when(serviciosExternos.estaLibre(anyLong(), any(), any())).thenReturn(true);
    }

    private PublicacionResumen crearPublicacion(Long id, String estado, int capacidad) {
        return new PublicacionResumen(id, "Apartamento " + id, "Bogota", "APARTAMENTO", capacidad,
                new BigDecimal("150000"), estado, List.of("Wi-Fi"));
    }

    // CA-053
    @Test
    void soloMuestraLasQueTienenCapacidadSuficiente() {
        when(serviciosExternos.buscarPublicacionesActivas("Bogota")).thenReturn(List.of(
                crearPublicacion(1L, "ACTIVA", 4),
                crearPublicacion(2L, "ACTIVA", 2)));

        BusquedaResponse respuesta = busquedaService.buscar(new BusquedaRequest("Bogota", llegada, salida, 3));

        assertEquals(1, respuesta.getResultados().size());
        assertEquals(1L, respuesta.getResultados().get(0).getId());
    }

    // CA-053
    @Test
    void noMuestraLasQueNoEstanLibresEnEsasFechas() {
        when(serviciosExternos.buscarPublicacionesActivas("Bogota")).thenReturn(List.of(
                crearPublicacion(1L, "ACTIVA", 4),
                crearPublicacion(2L, "ACTIVA", 4)));
        when(serviciosExternos.estaLibre(eq(2L), any(), any())).thenReturn(false);

        BusquedaResponse respuesta = busquedaService.buscar(new BusquedaRequest("Bogota", llegada, salida, 2));

        assertEquals(1, respuesta.getResultados().size());
        assertEquals(1L, respuesta.getResultados().get(0).getId());
    }

    // CA-054
    @Test
    void salidaIgualALlegadaDaError() {
        BusquedaException error = assertThrows(BusquedaException.class,
                () -> busquedaService.buscar(new BusquedaRequest("Bogota", llegada, llegada, 2)));
        assertEquals("La fecha de salida debe ser posterior a la fecha de llegada", error.getMessage());
    }

    // CA-054
    @Test
    void salidaAntesDeLlegadaDaError() {
        assertThrows(BusquedaException.class,
                () -> busquedaService.buscar(new BusquedaRequest("Bogota", salida, llegada, 2)));
    }

    // CA-055
    @Test
    void noMuestraPublicacionesQueNoEstanActivas() {
        when(serviciosExternos.buscarPublicacionesActivas("Bogota")).thenReturn(List.of(
                crearPublicacion(1L, "BORRADOR", 4),
                crearPublicacion(2L, "PENDIENTE_REVISION", 4),
                crearPublicacion(3L, "PAUSADA", 4),
                crearPublicacion(4L, "BLOQUEADA", 4),
                crearPublicacion(5L, "ACTIVA", 4)));

        BusquedaResponse respuesta = busquedaService.buscar(new BusquedaRequest("Bogota", llegada, salida, 2));

        assertEquals(1, respuesta.getResultados().size());
        assertEquals(5L, respuesta.getResultados().get(0).getId());
    }

    @Test
    void sinPublicacionesLaListaQuedaVacia() {
        when(serviciosExternos.buscarPublicacionesActivas("Bogota")).thenReturn(List.of());

        BusquedaResponse respuesta = busquedaService.buscar(new BusquedaRequest("Bogota", llegada, salida, 2));

        assertTrue(respuesta.getResultados().isEmpty());
    }

    @Test
    void laCiudadYLosHuespedesSonObligatorios() {
        BusquedaRequest datos = new BusquedaRequest("", llegada, salida, 0);
        assertEquals(2, validator.validate(datos).size());
    }

    @Test
    void elControllerDevuelveLosResultados() {
        when(serviciosExternos.buscarPublicacionesActivas("Bogota")).thenReturn(List.of(
                crearPublicacion(1L, "ACTIVA", 4)));

        BusquedaResponse respuesta = busquedaController.buscar(new BusquedaRequest("Bogota", llegada, salida, 2));

        assertEquals(1, respuesta.getResultados().size());
    }
}