package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.TarifasRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.ServicioAdicional;
import co.edu.unbosque.marketplace.alojamientos.model.TipoAlojamiento;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-10: tarifa de limpieza, descuento por estancia larga y servicios adicionales
@SpringBootTest
class TarifasPublicacionServiceTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Long ANFITRION = 7L;
    private static final Long OTRO_ANFITRION = 8L;

    @Autowired
    private TarifasPublicacionService tarifasPublicacionService;

    @Autowired
    private PublicacionRepository publicacionRepository;

    private Long idBorrador;

    @BeforeEach
    void preparar() {
        publicacionRepository.deleteAll();
        idBorrador = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();
    }

    private Publicacion publicacion(Long anfitrionId, EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion(anfitrionId, HOY);
        publicacion.definirDatosBasicos("Cabaña en el lago", "Con vista al lago", "Guatavita",
                "Vereda Tominé, km 3", TipoAlojamiento.CABANA, 4, new BigDecimal("250000"));
        ReflectionTestUtils.setField(publicacion, "estado", estado);
        return publicacionRepository.save(publicacion);
    }

    private Publicacion guardada(Long id) {
        return publicacionRepository.findById(id).orElseThrow();
    }

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    private static ServicioAdicionalDTO adicional(String nombre, String costo) {
        return new ServicioAdicionalDTO(nombre, costo == null ? null : new BigDecimal(costo));
    }

    private TarifasRequestDTO tarifas(String limpieza, String porcentaje, String noches,
            List<ServicioAdicionalDTO> adicionales) {
        return new TarifasRequestDTO(limpieza == null ? null : pesos(limpieza),
                porcentaje == null ? null : pesos(porcentaje), noches == null ? null : pesos(noches), adicionales);
    }

    // ---------- Decisión 8 ----------

    @Test
    @DisplayName("Decisión 8: una publicación nueva no tiene limpieza, descuento ni servicios adicionales")
    void valoresIniciales() {
        Publicacion publicacion = guardada(idBorrador);

        assertEquals(0, publicacion.getTarifaLimpieza().signum());
        assertEquals(0, publicacion.getPorcentajeDescuento());
        assertNull(publicacion.getNochesMinimasDescuento());
        assertTrue(publicacion.getServiciosAdicionales().isEmpty());
    }

    // ---------- CA-029, CA-030 y CA-129 ----------

    @Test
    @DisplayName("CA-029, CA-030 y CA-129 el anfitrión define limpieza, descuento y servicios adicionales")
    void defineTarifas() {
        // Dado que tengo una publicación, cuando defino las tarifas
        Publicacion publicacion = tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                tarifas("60000", "10", "7", List.of(adicional(" Desayuno ", "25000"), adicional("Traslado", "80000"))));

        // Entonces quedan guardadas para las cotizaciones
        Publicacion guardada = guardada(publicacion.getId());
        assertEquals(pesos("60000"), guardada.getTarifaLimpieza());
        assertEquals(10, guardada.getPorcentajeDescuento());
        assertEquals(7, guardada.getNochesMinimasDescuento());
        List<ServicioAdicional> adicionales = guardada.getServiciosAdicionales();
        assertEquals(2, adicionales.size());
        assertEquals("Desayuno", adicionales.get(0).getNombre());
        assertEquals(0, pesos("25000").compareTo(adicionales.get(0).getCosto()));
        assertEquals("Traslado", adicionales.get(1).getNombre());
    }

    @Test
    @DisplayName("Decisión 1: la nueva configuración reemplaza a la anterior")
    void reemplazaTarifas() {
        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                tarifas("60000", "10", "7", List.of(adicional("Desayuno", "25000"))));

        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("0", "0", null, List.of()));

        Publicacion publicacion = guardada(idBorrador);
        assertEquals(0, publicacion.getTarifaLimpieza().signum());
        assertEquals(0, publicacion.getPorcentajeDescuento());
        assertNull(publicacion.getNochesMinimasDescuento());
        assertTrue(publicacion.getServiciosAdicionales().isEmpty());
    }

    @Test
    @DisplayName("Decisión 3: un valor escrito con exponente o con .0 se guarda como entero en pesos")
    void guardaEnteros() {
        Publicacion publicacion = tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                tarifas("6E+4", "10.0", "7", List.of(adicional("Desayuno", "2.5E+4"))));

        assertEquals("60000", publicacion.getTarifaLimpieza().toPlainString());
        assertEquals("25000", publicacion.getServiciosAdicionales().get(0).getCosto().toPlainString());
        assertEquals(10, publicacion.getPorcentajeDescuento());
    }

    @ParameterizedTest(name = "porcentaje {0}")
    @ValueSource(strings = {"1", "90"})
    @DisplayName("Decisiones 4 y 5: se aceptan los extremos del porcentaje y de las noches mínimas")
    void aceptaExtremos(String porcentaje) {
        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("0", porcentaje, "2", List.of()));
        assertEquals(2, guardada(idBorrador).getNochesMinimasDescuento());

        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("0", porcentaje, "365", List.of()));
        assertEquals(365, guardada(idBorrador).getNochesMinimasDescuento());
    }

    // ---------- CA-031: valores negativos y fuera de rango ----------

    @ParameterizedTest(name = "limpieza {0}")
    @ValueSource(strings = {"-1", "-60000", "1500.5"})
    @DisplayName("CA-031 una tarifa de limpieza negativa o con decimales se rechaza y no cambia nada")
    void rechazaLimpiezaInvalida(String limpieza) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas(limpieza, "0", null, List.of())));

        assertEquals(TarifasRequestDTO.MENSAJE_LIMPIEZA, error.getMessage());
        assertEquals(0, guardada(idBorrador).getTarifaLimpieza().signum());
    }

    @ParameterizedTest(name = "porcentaje {0}")
    @ValueSource(strings = {"-1", "-10", "91", "100", "10.5"})
    @DisplayName("CA-031 un porcentaje negativo, mayor a 90 o con decimales se rechaza")
    void rechazaPorcentajeInvalido(String porcentaje) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas("0", porcentaje, "7", List.of())));

        assertEquals(TarifasRequestDTO.MENSAJE_PORCENTAJE, error.getMessage());
    }

    @ParameterizedTest(name = "noches {0}")
    @ValueSource(strings = {"-3", "0", "1", "366", "2.5"})
    @DisplayName("CA-031 y decisión 5: noches mínimas negativas, menores a 2, mayores a 365 o con decimales se rechazan")
    void rechazaNochesInvalidas(String noches) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas("0", "10", noches, List.of())));

        assertEquals(TarifasRequestDTO.MENSAJE_NOCHES, error.getMessage());
    }

    @ParameterizedTest(name = "costo {0}")
    @ValueSource(strings = {"-1", "-25000", "0", "100.5"})
    @DisplayName("CA-031 y decisión 6: un servicio adicional con costo negativo, cero o con decimales se rechaza")
    void rechazaCostoInvalido(String costo) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas("0", "0", null, List.of(adicional("Desayuno", costo)))));

        assertEquals(ServicioAdicionalDTO.MENSAJE_COSTO, error.getMessage());
        assertTrue(guardada(idBorrador).getServiciosAdicionales().isEmpty());
    }

    @Test
    @DisplayName("CA-031: sin tarifa de limpieza, sin porcentaje o sin costo se rechaza")
    void rechazaValoresAusentes() {
        assertThrows(SolicitudInvalidaException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                idBorrador, tarifas(null, "0", null, List.of())));
        assertThrows(SolicitudInvalidaException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                idBorrador, tarifas("0", null, null, List.of())));
        assertThrows(SolicitudInvalidaException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                idBorrador, tarifas("0", "0", null, List.of(adicional("Desayuno", null)))));
    }

    // ---------- Decisión 5: noches mínimas según el descuento ----------

    @Test
    @DisplayName("Decisión 5: con descuento las noches mínimas son obligatorias")
    void nochesObligatoriasConDescuento() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas("0", "10", null, List.of())));

        assertEquals("Indique las noches mínimas desde las que se aplica el descuento", error.getMessage());
    }

    @Test
    @DisplayName("Decisión 5: sin descuento no se definen noches mínimas")
    void sinNochesSinDescuento() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas("0", "0", "7", List.of())));

        assertEquals("Sin porcentaje de descuento no se definen noches mínimas", error.getMessage());
    }

    // ---------- Decisión 6: servicios adicionales ----------

    @Test
    @DisplayName("Decisión 6: dos servicios adicionales con el mismo nombre, sin distinguir mayúsculas, se rechazan")
    void rechazaNombresRepetidos() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("0", "0", null,
                        List.of(adicional("Desayuno", "25000"), adicional(" DESAYUNO ", "30000")))));

        assertEquals("No puede haber dos servicios adicionales con el mismo nombre", error.getMessage());
    }

    @ParameterizedTest(name = "nombre \"{0}\"")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Decisión 6: un servicio adicional sin nombre se rechaza")
    void rechazaNombreVacio(String nombre) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                        tarifas("0", "0", null, List.of(adicional(nombre, "25000")))));

        assertEquals(ServicioAdicionalDTO.MENSAJE_NOMBRE, error.getMessage());
    }

    @Test
    @DisplayName("Decisión 6: un nombre de 60 caracteres se acepta y uno de 61 se rechaza")
    void limiteDeNombre() {
        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador,
                tarifas("0", "0", null, List.of(adicional("a".repeat(60), "1"))));
        assertEquals(60, guardada(idBorrador).getServiciosAdicionales().get(0).getNombre().length());

        assertThrows(SolicitudInvalidaException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                idBorrador, tarifas("0", "0", null, List.of(adicional("a".repeat(61), "1")))));
    }

    @Test
    @DisplayName("Decisión 6: se aceptan 10 servicios adicionales y se rechazan 11")
    void limiteDeCantidad() {
        List<ServicioAdicionalDTO> diez = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> adicional("Servicio " + i, "1000")).toList();
        List<ServicioAdicionalDTO> once = IntStream.rangeClosed(1, 11)
                .mapToObj(i -> adicional("Servicio " + i, "1000")).toList();

        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("0", "0", null, diez));
        assertEquals(10, guardada(idBorrador).getServiciosAdicionales().size());

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("0", "0", null, once)));
        assertEquals(TarifasRequestDTO.MENSAJE_CANTIDAD_ADICIONALES, error.getMessage());
    }

    @Test
    @DisplayName("Decisión 6: sin lista de servicios adicionales o con un elemento nulo se rechaza")
    void rechazaListaNula() {
        assertThrows(SolicitudInvalidaException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                idBorrador, tarifas("0", "0", null, null)));
        assertThrows(SolicitudInvalidaException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                idBorrador, tarifas("0", "0", null, Arrays.asList(adicional("Desayuno", "1000"), null))));
    }

    // ---------- Decisión 2: dueño y estado ----------

    @Test
    @DisplayName("Decisión 2: la publicación de otro anfitrión o una que no existe responde 404")
    void publicacionAjenaONoExiste() {
        assertThrows(NoEncontradoException.class, () -> tarifasPublicacionService.definirTarifas(OTRO_ANFITRION,
                idBorrador, tarifas("60000", "0", null, List.of())));
        assertThrows(NoEncontradoException.class, () -> tarifasPublicacionService.definirTarifas(ANFITRION,
                999999L, tarifas("60000", "0", null, List.of())));

        assertEquals(0, guardada(idBorrador).getTarifaLimpieza().signum());
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"BORRADOR", "ACTIVA", "PAUSADA"})
    @DisplayName("Decisión 2: en BORRADOR, ACTIVA y PAUSADA se puede cambiar y el estado no cambia")
    void permiteEstadosEditables(EstadoPublicacion estado) {
        Long id = publicacion(ANFITRION, estado).getId();

        tarifasPublicacionService.definirTarifas(ANFITRION, id, tarifas("60000", "0", null, List.of()));

        assertEquals(estado, guardada(id).getEstado());
        assertEquals(pesos("60000"), guardada(id).getTarifaLimpieza());
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"PENDIENTE_REVISION", "BLOQUEADA"})
    @DisplayName("Decisión 2: en PENDIENTE_REVISION y BLOQUEADA no se puede cambiar (409)")
    void rechazaEstadosNoEditables(EstadoPublicacion estado) {
        Long id = publicacion(ANFITRION, estado).getId();

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> tarifasPublicacionService.definirTarifas(ANFITRION, id, tarifas("60000", "0", null, List.of())));

        assertEquals("Solo se pueden cambiar las tarifas de una publicación en BORRADOR, ACTIVA o PAUSADA",
                error.getMessage());
        assertEquals(0, guardada(id).getTarifaLimpieza().signum());
    }

    @Test
    @DisplayName("HU-10: definir tarifas no cambia los servicios, reglas ni imágenes de la HU-09")
    void noTocaElContenido() {
        Publicacion publicacion = guardada(idBorrador);
        publicacion.definirServicios(List.of("Wifi"));
        publicacion.definirReglas(List.of("No fumar"));
        publicacionRepository.save(publicacion);

        tarifasPublicacionService.definirTarifas(ANFITRION, idBorrador, tarifas("60000", "0", null, List.of()));

        assertEquals(List.of("Wifi"), guardada(idBorrador).getServicios());
        assertEquals(List.of("No fumar"), guardada(idBorrador).getReglas());
    }
}
