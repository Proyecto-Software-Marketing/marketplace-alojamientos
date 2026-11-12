package co.edu.unbosque.marketplace.busqueda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;

class DtoTest {

    @Test
    void publicacionResumenGuardaSusDatos() {
        PublicacionResumen publicacion = new PublicacionResumen();
        publicacion.setId(1L);
        publicacion.setTitulo("Casa en el centro");
        publicacion.setCiudad("Bogota");
        publicacion.setTipo("CASA");
        publicacion.setCapacidad(4);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado("ACTIVA");
        publicacion.setServicios(List.of("Wi-Fi", "Cocina"));

        assertEquals(1L, publicacion.getId());
        assertEquals("Casa en el centro", publicacion.getTitulo());
        assertEquals("Bogota", publicacion.getCiudad());
        assertEquals("CASA", publicacion.getTipo());
        assertEquals(4, publicacion.getCapacidad());
        assertEquals(new BigDecimal("100000"), publicacion.getPrecioNoche());
        assertEquals("ACTIVA", publicacion.getEstado());
        assertEquals(List.of("Wi-Fi", "Cocina"), publicacion.getServicios());
    }

    @Test
    void busquedaRequestGuardaSusDatos() {
        BusquedaRequest datos = new BusquedaRequest();
        datos.setCiudad("Medellin");
        datos.setLlegada(LocalDate.of(2026, 12, 1));
        datos.setSalida(LocalDate.of(2026, 12, 3));
        datos.setHuespedes(2);

        assertEquals("Medellin", datos.getCiudad());
        assertEquals(LocalDate.of(2026, 12, 1), datos.getLlegada());
        assertEquals(LocalDate.of(2026, 12, 3), datos.getSalida());
        assertEquals(2, datos.getHuespedes());
    }
}