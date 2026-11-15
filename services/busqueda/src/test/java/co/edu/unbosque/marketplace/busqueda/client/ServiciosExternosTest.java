package co.edu.unbosque.marketplace.busqueda.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.busqueda.dto.PublicacionDetalle;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;

// Pruebas de las llamadas a Alojamientos y Disponibilidad  simulado
class ServiciosExternosTest {

    private MockRestServiceServer servidor;
    private ServiciosExternos serviciosExternos;

    private final LocalDate llegada = LocalDate.of(2026, 12, 1);
    private final LocalDate salida = LocalDate.of(2026, 12, 5);

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        serviciosExternos = new ServiciosExternos(constructor, "http://alojamientos", "http://disponibilidad");
    }

    @Test
    void traeLasPublicacionesActivasDeAlojamientos() {
        servidor.expect(requestTo("http://alojamientos/interno/publicaciones/activas?ciudad=Bogota"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":1,\"titulo\":\"Casa\",\"ciudad\":\"Bogota\",\"tipo\":\"CASA\","
                        + "\"capacidad\":4,\"precioNoche\":100000,\"estado\":\"ACTIVA\",\"servicios\":[\"Wi-Fi\"]}]",
                        MediaType.APPLICATION_JSON));

        List<PublicacionResumen> publicaciones = serviciosExternos.buscarPublicacionesActivas("Bogota");

        assertEquals(1, publicaciones.size());
        assertEquals("Casa", publicaciones.get(0).getTitulo());
        servidor.verify();
    }

    @Test
    void traeElDetalleDeAlojamientos() {
        servidor.expect(requestTo("http://alojamientos/interno/publicaciones/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":5,\"titulo\":\"Casa\",\"estado\":\"ACTIVA\","
                        + "\"reglas\":[\"No fumar\"],\"politicaCancelacion\":\"FLEXIBLE\"}",
                        MediaType.APPLICATION_JSON));

        PublicacionDetalle detalle = serviciosExternos.obtenerDetalle(5L);

        assertEquals(5L, detalle.getId());
        assertEquals(List.of("No fumar"), detalle.getReglas());
        assertEquals("FLEXIBLE", detalle.getPoliticaCancelacion());
        servidor.verify();
    }

    @Test
    void siAlojamientosRespondeNoEncontradoElDetalleEsNulo() {
        servidor.expect(requestTo("http://alojamientos/interno/publicaciones/99"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertNull(serviciosExternos.obtenerDetalle(99L));
        servidor.verify();
    }

    @Test
    void siDisponibilidadLaDevuelveEstaLibre() {
        servidor.expect(requestTo("http://disponibilidad/internal/availability/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"disponibles\":[5]}", MediaType.APPLICATION_JSON));

        assertTrue(serviciosExternos.estaLibre(5L, llegada, salida));
        servidor.verify();
    }

    @Test
    void siDisponibilidadNoLaDevuelveNoEstaLibre() {
        servidor.expect(requestTo("http://disponibilidad/internal/availability/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"disponibles\":[]}", MediaType.APPLICATION_JSON));

        assertFalse(serviciosExternos.estaLibre(5L, llegada, salida));
        servidor.verify();
    }
}