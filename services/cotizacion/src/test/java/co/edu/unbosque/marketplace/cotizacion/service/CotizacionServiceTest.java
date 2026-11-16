package co.edu.unbosque.marketplace.cotizacion.service;

import co.edu.unbosque.marketplace.cotizacion.client.ClienteCotizacion;
import co.edu.unbosque.marketplace.cotizacion.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.cotizacion.dto.DatosPublicacion;
import co.edu.unbosque.marketplace.cotizacion.dto.ServicioAdicionalCotizado;
import co.edu.unbosque.marketplace.cotizacion.exception.CapacidadExcedidaException;
import co.edu.unbosque.marketplace.cotizacion.exception.ConflictoException;
import co.edu.unbosque.marketplace.cotizacion.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.cotizacion.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.cotizacion.model.ConfiguracionTarifa;
import co.edu.unbosque.marketplace.cotizacion.model.Cotizacion;
import co.edu.unbosque.marketplace.cotizacion.repository.ConfiguracionTarifaRepository;
import co.edu.unbosque.marketplace.cotizacion.repository.CotizacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la HU-22: cotización desglosada. Alojamientos se simula con ClienteCotizacion
@SpringBootTest
class CotizacionServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final Long HUESPED = 2L;
    private static final Long PUBLICACION = 1L;

    @Autowired
    private CotizacionService cotizacionService;

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private ConfiguracionTarifaRepository configuracionTarifaRepository;

    @MockitoBean
    private ClienteCotizacion clienteCotizacion;

    @MockitoBean
    private Clock reloj;

    @BeforeEach
    void preparar() {
        cotizacionRepository.deleteAll();
        configuracionTarifaRepository.deleteAll();
        when(reloj.getZone()).thenReturn(BOGOTA);
        when(reloj.instant()).thenReturn(HOY.atTime(10, 0).atZone(BOGOTA).toInstant());
        // Publicación ACTIVA de la HU-10: 250.000 por noche, limpieza 60.000, 10 % desde 7 noches
        when(clienteCotizacion.consultarDatosPublicacion(PUBLICACION)).thenReturn(publicacion("ACTIVA"));
    }

    private static DatosPublicacion publicacion(String estado) {
        DatosPublicacion datos = new DatosPublicacion();
        datos.setPublicacionId(PUBLICACION);
        datos.setEstado(estado);
        datos.setCapacidad(4);
        datos.setPrecioPorNoche(new BigDecimal("250000"));
        datos.setTarifaLimpieza(new BigDecimal("60000"));
        datos.setPorcentajeDescuentoEstanciaLarga(10);
        datos.setNochesMinimasDescuento(7);
        datos.setServiciosAdicionales(List.of(
                new ServicioAdicionalCotizado("Desayuno", new BigDecimal("25000")),
                new ServicioAdicionalCotizado("Traslado al aeropuerto", new BigDecimal("80000"))));
        return datos;
    }

    // El administrador configura la tarifa de servicio (HU-38)
    private void tarifaServicio(String porcentaje) {
        configuracionTarifaRepository.save(new ConfiguracionTarifa(new BigDecimal(porcentaje), 1L));
    }

    private static CotizacionRequest solicitud(int diasHastaLlegada, int noches, int huespedes,
            List<String> adicionales) {
        LocalDate llegada = HOY.plusDays(diasHastaLlegada);
        return new CotizacionRequest(PUBLICACION, llegada, llegada.plusDays(noches), huespedes, adicionales);
    }

    private static void assertPesos(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " y fue " + real);
    }

    // ---------- CA-060 y CA-061 ----------

    @Test
    @DisplayName("CA-060 y CA-061 la cotización trae el desglose completo y el total cumple la fórmula")
    void cotizacionDesglosada() {
        tarifaServicio("15");

        // Dado que elegí 7 noches para 3 huéspedes con desayuno
        Cotizacion cotizacion = cotizacionService.cotizar(HUESPED, solicitud(10, 7, 3, List.of("Desayuno")));

        // Entonces veo noches, precio base, limpieza, adicionales, descuento, tarifa de servicio y total
        assertEquals(7, cotizacion.getNoches());
        assertPesos("250000", cotizacion.getPrecioPorNoche());
        assertPesos("1750000", cotizacion.getPrecioBase());
        assertPesos("60000", cotizacion.getTarifaLimpieza());
        assertEquals(1, cotizacion.getServiciosAdicionales().size());
        assertEquals("Desayuno", cotizacion.getServiciosAdicionales().get(0).getNombre());
        assertPesos("25000", cotizacion.getServiciosAdicionales().get(0).getCosto());
        assertEquals(10, cotizacion.getPorcentajeDescuento());
        assertPesos("175000", cotizacion.getDescuento());
        // Subtotal 1.750.000 + 60.000 + 25.000 − 175.000 = 1.660.000; 15 % = 249.000
        assertPesos("15", cotizacion.getPorcentajeTarifaServicio());
        assertPesos("249000", cotizacion.getTarifaServicio());
        assertPesos("1909000", cotizacion.getTotal());
    }

    @Test
    @DisplayName("CA-060 la cotización queda guardada con la publicación, las fechas, los huéspedes y el huésped")
    void quedaGuardada() {
        CotizacionRequest datos = solicitud(10, 3, 2, List.of());

        String id = cotizacionService.cotizar(HUESPED, datos).getId();

        Cotizacion guardada = cotizacionService.consultar(id);
        assertEquals(36, id.length());
        assertEquals(PUBLICACION, guardada.getPublicacionId());
        assertEquals(HUESPED, guardada.getIdHuesped());
        assertEquals(datos.getFechaLlegada(), guardada.getFechaLlegada());
        assertEquals(datos.getFechaSalida(), guardada.getFechaSalida());
        assertEquals(2, guardada.getCantidadHuespedes());
        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 0), guardada.getCreadaEn());
    }

    @Test
    @DisplayName("CA-061 sin tarifa de servicio configurada, limpieza ni adicionales el total es solo el de las noches")
    void totalSoloNoches() {
        DatosPublicacion sinExtras = publicacion("ACTIVA");
        sinExtras.setTarifaLimpieza(BigDecimal.ZERO);
        sinExtras.setPorcentajeDescuentoEstanciaLarga(0);
        sinExtras.setNochesMinimasDescuento(null);
        when(clienteCotizacion.consultarDatosPublicacion(PUBLICACION)).thenReturn(sinExtras);

        Cotizacion cotizacion = cotizacionService.cotizar(HUESPED, solicitud(0, 2, 1, List.of()));

        assertPesos("500000", cotizacion.getPrecioBase());
        assertPesos("0", cotizacion.getDescuento());
        assertPesos("0", cotizacion.getTarifaServicio());
        assertPesos("500000", cotizacion.getTotal());
        assertTrue(cotizacion.getServiciosAdicionales().isEmpty());
    }

    @Test
    @DisplayName("Decisión 9: el descuento y la tarifa de servicio se redondean al peso más cercano")
    void redondeo() {
        DatosPublicacion datos = publicacion("ACTIVA");
        datos.setPrecioPorNoche(new BigDecimal("33333"));
        datos.setTarifaLimpieza(BigDecimal.ZERO);
        datos.setNochesMinimasDescuento(2);
        when(clienteCotizacion.consultarDatosPublicacion(PUBLICACION)).thenReturn(datos);
        tarifaServicio("12.5");

        Cotizacion cotizacion = cotizacionService.cotizar(HUESPED, solicitud(1, 3, 1, List.of()));

        // Base 99.999; 10 % = 9.999,9 → 10.000; subtotal 89.999; 12,5 % = 11.249,875 → 11.250
        assertPesos("99999", cotizacion.getPrecioBase());
        assertPesos("10000", cotizacion.getDescuento());
        assertPesos("11250", cotizacion.getTarifaServicio());
        assertPesos("101249", cotizacion.getTotal());
    }

    // ---------- CA-062 ----------

    @ParameterizedTest(name = "{0} noches → descuento {1}")
    @CsvSource({"6, 0, 0", "7, 10, 175000", "8, 10, 200000"})
    @DisplayName("CA-062 el descuento se aplica solo si la estancia alcanza las noches mínimas del anfitrión")
    void descuentoPorEstanciaLarga(int noches, int porcentaje, String descuento) {
        Cotizacion cotizacion = cotizacionService.cotizar(HUESPED, solicitud(5, noches, 2, List.of()));

        assertEquals(porcentaje, cotizacion.getPorcentajeDescuento());
        assertPesos(descuento, cotizacion.getDescuento());
    }

    @Test
    @DisplayName("CA-062 el descuento no se aplica a la limpieza ni a los servicios adicionales")
    void descuentoSoloNoches() {
        Cotizacion cotizacion = cotizacionService.cotizar(HUESPED,
                solicitud(5, 7, 2, List.of("Desayuno", "Traslado al aeropuerto")));

        // 10 % de 1.750.000; la limpieza (60.000) y los adicionales (105.000) se cobran completos
        assertPesos("175000", cotizacion.getDescuento());
        assertPesos("1740000", cotizacion.getTotal());
    }

    // ---------- CA-063 ----------

    @Test
    @DisplayName("CA-063 más huéspedes que la capacidad no genera la cotización e indica el motivo")
    void superaCapacidad() {
        CapacidadExcedidaException error = assertThrows(CapacidadExcedidaException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(5, 3, 5, List.of())));

        assertEquals("La publicación admite máximo 4 huéspedes", error.getMessage());
        assertEquals(0, cotizacionRepository.count());
    }

    @Test
    @DisplayName("CA-063 exactamente la capacidad sí se cotiza")
    void justoLaCapacidad() {
        assertEquals(4, cotizacionService.cotizar(HUESPED, solicitud(5, 3, 4, List.of())).getCantidadHuespedes());
    }

    // ---------- CA-129 y decisión 7: servicios adicionales ----------

    @Test
    @DisplayName("CA-129 los servicios adicionales elegidos se suman, sin distinguir mayúsculas y una vez cada uno")
    void serviciosAdicionales() {
        Cotizacion cotizacion = cotizacionService.cotizar(HUESPED,
                solicitud(5, 2, 2, List.of(" desayuno ", "TRASLADO AL AEROPUERTO", "Desayuno")));

        assertEquals(2, cotizacion.getServiciosAdicionales().size());
        assertEquals("Desayuno", cotizacion.getServiciosAdicionales().get(0).getNombre());
        assertEquals("Traslado al aeropuerto", cotizacion.getServiciosAdicionales().get(1).getNombre());
        // 500.000 + 60.000 + 25.000 + 80.000
        assertPesos("665000", cotizacion.getTotal());
    }

    @Test
    @DisplayName("Decisión 7: un servicio adicional que la publicación no ofrece se rechaza")
    void servicioNoOfrecido() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(5, 2, 2, List.of("Spa"))));

        assertEquals("La publicación no ofrece el servicio adicional: Spa", error.getMessage());
        assertEquals(0, cotizacionRepository.count());
    }

    @ParameterizedTest(name = "servicio \"{0}\"")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Decisión 7: un servicio adicional vacío se rechaza")
    void servicioVacio(String servicio) {
        assertThrows(SolicitudInvalidaException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(5, 2, 2, List.of(servicio))));
    }

    // ---------- Decisión 4: fechas ----------

    @Test
    @DisplayName("Decisión 4: llegar hoy es válido; llegar ayer no")
    void llegadaDesdeHoy() {
        assertEquals(HOY, cotizacionService.cotizar(HUESPED, solicitud(0, 1, 1, List.of())).getFechaLlegada());

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(-1, 2, 1, List.of())));
        assertEquals("La fecha de llegada no puede ser anterior a hoy", error.getMessage());
    }

    @ParameterizedTest(name = "{0} noches")
    @ValueSource(ints = {0, -2})
    @DisplayName("Decisión 4: la salida debe ser posterior a la llegada")
    void salidaPosterior(int noches) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(5, noches, 1, List.of())));

        assertEquals("La fecha de salida debe ser posterior a la de llegada", error.getMessage());
        verify(clienteCotizacion, never()).consultarDatosPublicacion(anyLong());
    }

    @Test
    @DisplayName("Decisión 4: se aceptan 365 noches y se rechazan 366")
    void maximoDeNoches() {
        assertEquals(365, cotizacionService.cotizar(HUESPED, solicitud(5, 365, 1, List.of())).getNoches());

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(5, 366, 1, List.of())));
        assertEquals("La estancia puede ser de máximo 365 noches", error.getMessage());
    }

    // ---------- Decisión 5: estado de la publicación ----------

    @ParameterizedTest(name = "estado {0}")
    @ValueSource(strings = {"BORRADOR", "PENDIENTE_REVISION", "PAUSADA", "BLOQUEADA"})
    @DisplayName("Decisión 5: una publicación que no está ACTIVA no se cotiza")
    void publicacionNoActiva(String estado) {
        when(clienteCotizacion.consultarDatosPublicacion(PUBLICACION)).thenReturn(publicacion(estado));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> cotizacionService.cotizar(HUESPED, solicitud(5, 2, 1, List.of())));

        assertEquals("La publicación no está disponible para cotizar", error.getMessage());
        assertEquals(0, cotizacionRepository.count());
    }

    // ---------- Decisión 1: consultar ----------

    @Test
    @DisplayName("Decisión 1: consultar una cotización que no existe responde 404")
    void consultarNoExiste() {
        NoEncontradoException error = assertThrows(NoEncontradoException.class,
                () -> cotizacionService.consultar("no-existe"));

        assertEquals("La cotización no existe", error.getMessage());
    }
}
