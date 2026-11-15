package co.edu.unbosque.marketplace.busqueda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.edu.unbosque.marketplace.busqueda.client.ServiciosExternos;
import co.edu.unbosque.marketplace.busqueda.controller.BusquedaController;
import co.edu.unbosque.marketplace.busqueda.dto.DetalleResponse;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionDetalle;
import co.edu.unbosque.marketplace.busqueda.exception.PublicacionNoEncontradaException;

// Pruebas del detalle de una publicacion
@SpringBootTest
class DetalleTest {

    @Autowired
    private BusquedaService busquedaService;

    @Autowired
    private BusquedaController busquedaController;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private final LocalDate llegada = LocalDate.of(2026, 12, 1);
    private final LocalDate salida = LocalDate.of(2026, 12, 5);

    private PublicacionDetalle crearDetalle(Long id, String estado) {
        PublicacionDetalle detalle = new PublicacionDetalle();
        detalle.setId(id);
        detalle.setTitulo("Casa con jardín");
        detalle.setDescripcion("Casa amplia cerca al parque");
        detalle.setCiudad("Bogota");
        detalle.setTipo("CASA");
        detalle.setCapacidad(4);
        detalle.setPrecioNoche(new BigDecimal("120000"));
        detalle.setEstado(estado);
        detalle.setServicios(List.of("Wi-Fi", "Cocina"));
        detalle.setReglas(List.of("No fumar", "No mascotas"));
        detalle.setImagenes(List.of("https://imagenes.com/casa1.jpg"));
        detalle.setPoliticaCancelacion("FLEXIBLE");
        return detalle;
    }

    // CA-058
    @Test
    void muestraLaInformacionCompletaDeLaPublicacion() {
        when(serviciosExternos.obtenerDetalle(5L)).thenReturn(crearDetalle(5L, "ACTIVA"));

        PublicacionDetalle publicacion = busquedaService.detalle(5L, llegada, salida, 2).getPublicacion();

        assertEquals(5L, publicacion.getId());
        assertEquals("Casa con jardín", publicacion.getTitulo());
        assertEquals("Casa amplia cerca al parque", publicacion.getDescripcion());
        assertEquals("Bogota", publicacion.getCiudad());
        assertEquals("CASA", publicacion.getTipo());
        assertEquals(4, publicacion.getCapacidad());
        assertEquals(new BigDecimal("120000"), publicacion.getPrecioNoche());
        assertEquals("ACTIVA", publicacion.getEstado());
        assertEquals(List.of("Wi-Fi", "Cocina"), publicacion.getServicios());
        assertEquals(List.of("No fumar", "No mascotas"), publicacion.getReglas());
        assertEquals(List.of("https://imagenes.com/casa1.jpg"), publicacion.getImagenes());
        assertEquals("FLEXIBLE", publicacion.getPoliticaCancelacion());
    }

    // CA-059
    @Test
    void devuelveLosDatosDeLaBusquedaParaPasarACotizar() {
        when(serviciosExternos.obtenerDetalle(5L)).thenReturn(crearDetalle(5L, "ACTIVA"));

        DetalleResponse respuesta = busquedaService.detalle(5L, llegada, salida, 2);

        assertEquals(llegada, respuesta.getLlegada());
        assertEquals(salida, respuesta.getSalida());
        assertEquals(2, respuesta.getHuespedes());
    }

    @Test
    void sinFechasTambienMuestraElDetalle() {
        when(serviciosExternos.obtenerDetalle(5L)).thenReturn(crearDetalle(5L, "ACTIVA"));

        DetalleResponse respuesta = busquedaController.detalle(5L, null, null, null);

        assertEquals(5L, respuesta.getPublicacion().getId());
        assertNull(respuesta.getLlegada());
    }

    @Test
    void unaPublicacionQueNoEstaActivaNoSeMuestra() {
        when(serviciosExternos.obtenerDetalle(5L)).thenReturn(crearDetalle(5L, "PAUSADA"));

        PublicacionNoEncontradaException error = assertThrows(PublicacionNoEncontradaException.class,
                () -> busquedaService.detalle(5L, llegada, salida, 2));
        assertEquals("La publicación no existe o no está disponible", error.getMessage());
    }

    @Test
    void unaPublicacionQueNoExisteDaError() {
        when(serviciosExternos.obtenerDetalle(99L)).thenReturn(null);

        assertThrows(PublicacionNoEncontradaException.class,
                () -> busquedaService.detalle(99L, llegada, salida, 2));
    }
}