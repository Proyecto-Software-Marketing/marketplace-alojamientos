package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.CatalogoServicios;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pruebas de la HU-09: servicios, reglas e imágenes de la publicación
@SpringBootTest
class ContenidoPublicacionServiceTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Long ANFITRION = 7L;
    private static final Long OTRO_ANFITRION = 8L;

    @Autowired
    private ContenidoPublicacionService contenidoPublicacionService;

    @Autowired
    private PublicacionRepository publicacionRepository;

    private Long idBorrador;

    @BeforeEach
    void preparar() {
        publicacionRepository.deleteAll();
        idBorrador = publicacion(ANFITRION, EstadoPublicacion.BORRADOR).getId();
    }

    // La aprobación y la pausa llegan con otras historias; las pruebas fijan el estado directamente
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

    // ---------- CA-026: servicios ----------

    @Test
    @DisplayName("CA-026 agregar servicios los guarda en la publicación")
    void agregaServicios() {
        // Dado que tengo una publicación, cuando agrego servicios
        Publicacion publicacion = contenidoPublicacionService.definirServicios(ANFITRION, idBorrador,
                List.of("Wifi", "Cocina"));

        // Entonces quedan en la publicación
        assertEquals(List.of("Wifi", "Cocina"), publicacion.getServicios());
        assertEquals(List.of("Wifi", "Cocina"), guardada(idBorrador).getServicios());
    }

    @Test
    @DisplayName("CA-026 quitar servicios: la nueva lista reemplaza a la anterior")
    void quitaServicios() {
        contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, List.of("Wifi", "Cocina", "Piscina"));

        contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, List.of("Piscina"));

        assertEquals(List.of("Piscina"), guardada(idBorrador).getServicios());
    }

    @Test
    @DisplayName("Decisión 2: una lista vacía quita todos los servicios")
    void quitaTodosLosServicios() {
        contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, List.of("Wifi"));

        contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, List.of());

        assertTrue(guardada(idBorrador).getServicios().isEmpty());
    }

    @Test
    @DisplayName("Decisión 6: los servicios se aceptan sin distinguir mayúsculas y se guardan como en el catálogo")
    void normalizaServicios() {
        Publicacion publicacion = contenidoPublicacionService.definirServicios(ANFITRION, idBorrador,
                List.of("wifi", "  AIRE ACONDICIONADO ", "piscina"));

        assertEquals(List.of("Wifi", "Aire acondicionado", "Piscina"), publicacion.getServicios());
    }

    @Test
    @DisplayName("Decisión 7: sin repetidos y en el orden del catálogo")
    void ordenaYQuitaRepetidos() {
        Publicacion publicacion = contenidoPublicacionService.definirServicios(ANFITRION, idBorrador,
                List.of("Piscina", "Wifi", "WIFI", "Cocina", "Piscina"));

        assertEquals(List.of("Wifi", "Cocina", "Piscina"), publicacion.getServicios());
    }

    @Test
    @DisplayName("Decisión 5: se aceptan todos los servicios del catálogo")
    void aceptaTodoElCatalogo() {
        Publicacion publicacion = contenidoPublicacionService.definirServicios(ANFITRION, idBorrador,
                CatalogoServicios.VALORES);

        assertEquals(CatalogoServicios.VALORES, publicacion.getServicios());
    }

    @ParameterizedTest(name = "servicio \"{0}\"")
    @ValueSource(strings = {"Jacuzzi", "Wi-Fi", "", "   "})
    @DisplayName("Decisión 5: un servicio fuera del catálogo se rechaza y no cambia nada")
    void rechazaServicioFueraDelCatalogo(String servicio) {
        contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, List.of("Wifi"));

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirServicios(ANFITRION, idBorrador,
                        List.of("Cocina", servicio)));

        assertEquals("Cada servicio debe ser uno de: Wifi, Cocina, Aire acondicionado, Lavadora, "
                + "Aparcamiento, Piscina", error.getMessage());
        assertEquals(List.of("Wifi"), guardada(idBorrador).getServicios());
    }

    @Test
    @DisplayName("Decisión 2: sin lista de servicios o con un servicio nulo se rechaza")
    void rechazaServiciosNulos() {
        List<String> conNulo = new ArrayList<>();
        conNulo.add(null);

        assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, null));
        assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, conNulo));
    }

    // ---------- CA-027: reglas ----------

    @Test
    @DisplayName("CA-027 las reglas quedan registradas en el orden en que se escribieron")
    void registraReglas() {
        // Dado que tengo una publicación, cuando registro las reglas del alojamiento
        Publicacion publicacion = contenidoPublicacionService.definirReglas(ANFITRION, idBorrador,
                List.of("No fumar", "  No se admiten mascotas ", "Silencio después de las 10 p. m."));

        // Entonces quedan en la publicación, sin espacios sobrantes
        List<String> esperadas = List.of("No fumar", "No se admiten mascotas", "Silencio después de las 10 p. m.");
        assertEquals(esperadas, publicacion.getReglas());
        assertEquals(esperadas, guardada(idBorrador).getReglas());
    }

    @Test
    @DisplayName("Decisiones 2 y 9: las reglas nuevas reemplazan a las anteriores y las repetidas se guardan una vez")
    void reemplazaReglas() {
        contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, List.of("No fumar", "No fiestas"));

        contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, List.of("No fiestas", "No fiestas"));

        assertEquals(List.of("No fiestas"), guardada(idBorrador).getReglas());
    }

    @ParameterizedTest(name = "regla \"{0}\"")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Decisión 8: una regla vacía o en blanco se rechaza")
    void rechazaReglaVacia(String regla) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, List.of("No fumar", regla)));

        assertEquals("Las reglas no pueden estar vacías", error.getMessage());
        assertTrue(guardada(idBorrador).getReglas().isEmpty());
    }

    @Test
    @DisplayName("Decisión 8: una regla de más de 200 caracteres se rechaza; de 200 se acepta")
    void limiteDeLongitudDeRegla() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, List.of("a".repeat(201))));
        assertEquals("Cada regla puede tener máximo 200 caracteres", error.getMessage());

        Publicacion publicacion = contenidoPublicacionService.definirReglas(ANFITRION, idBorrador,
                List.of("a".repeat(200)));
        assertEquals(200, publicacion.getReglas().get(0).length());
    }

    @Test
    @DisplayName("Decisión 8: se aceptan 20 reglas distintas y se rechazan 21")
    void limiteDeCantidadDeReglas() {
        List<String> veinte = IntStream.rangeClosed(1, 20).mapToObj(i -> "Regla " + i).toList();
        List<String> veintiuna = IntStream.rangeClosed(1, 21).mapToObj(i -> "Regla " + i).toList();

        assertEquals(20, contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, veinte).getReglas().size());
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, veintiuna));
        assertEquals("Se pueden registrar máximo 20 reglas", error.getMessage());
    }

    // ---------- CA-028: imágenes ----------

    @Test
    @DisplayName("CA-028 las referencias de imágenes quedan registradas en el orden recibido")
    void registraImagenes() {
        // Dado que tengo una publicación, cuando agrego rutas o referencias de imágenes
        List<String> imagenes = List.of("https://fotos.ejemplo.com/cabana-1.jpg", "/imagenes/cabana-2.png",
                "http://fotos.ejemplo.com/cabana-3.jpg");

        Publicacion publicacion = contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, imagenes);

        // Entonces quedan en la publicación; la primera es la foto principal
        assertEquals(imagenes, publicacion.getImagenes());
        assertEquals(imagenes, guardada(idBorrador).getImagenes());
    }

    @Test
    @DisplayName("Decisión 12: las imágenes repetidas se guardan una vez y se quitan los espacios sobrantes")
    void quitaImagenesRepetidas() {
        Publicacion publicacion = contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador,
                List.of(" /img/a.jpg ", "/img/b.jpg", "/img/a.jpg"));

        assertEquals(List.of("/img/a.jpg", "/img/b.jpg"), publicacion.getImagenes());
    }

    @ParameterizedTest(name = "imagen \"{0}\"")
    @ValueSource(strings = {"cabana.jpg", "ftp://fotos/a.jpg", "https://", "/", "/img/mi foto.jpg", "", "   ",
            "C:\\fotos\\a.jpg"})
    @DisplayName("Decisión 11: una referencia que no empieza por http://, https:// o / o que tiene espacios se rechaza")
    void rechazaImagenInvalida(String imagen) {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, List.of(imagen)));

        assertEquals("Cada imagen debe ser una dirección que empiece por http://, https:// o / y no tenga espacios",
                error.getMessage());
        assertTrue(guardada(idBorrador).getImagenes().isEmpty());
    }

    @Test
    @DisplayName("Decisión 10: una imagen de más de 500 caracteres se rechaza")
    void rechazaImagenLarga() {
        String larga = "https://fotos.ejemplo.com/" + "a".repeat(480);

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, List.of(larga)));
        assertEquals("Cada imagen puede tener máximo 500 caracteres", error.getMessage());
    }

    @Test
    @DisplayName("Decisión 10: se aceptan 10 imágenes y se rechazan 11")
    void limiteDeCantidadDeImagenes() {
        List<String> diez = IntStream.rangeClosed(1, 10).mapToObj(i -> "/img/" + i + ".jpg").toList();
        List<String> once = IntStream.rangeClosed(1, 11).mapToObj(i -> "/img/" + i + ".jpg").toList();

        assertEquals(10, contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, diez).getImagenes().size());
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, once));
        assertEquals("Se pueden registrar máximo 10 imágenes", error.getMessage());
    }

    @Test
    @DisplayName("Decisión 2: sin lista de reglas o de imágenes se rechaza")
    void rechazaListasNulas() {
        assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, null));
        assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, null));
        assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, Arrays.asList("/a.jpg", null)));
    }

    // ---------- Las tres listas son independientes ----------

    @Test
    @DisplayName("HU-09: cambiar una lista no toca las otras dos")
    void listasIndependientes() {
        contenidoPublicacionService.definirServicios(ANFITRION, idBorrador, List.of("Wifi"));
        contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, List.of("No fumar"));
        contenidoPublicacionService.definirImagenes(ANFITRION, idBorrador, List.of("/img/a.jpg"));

        contenidoPublicacionService.definirReglas(ANFITRION, idBorrador, List.of("No fiestas"));

        Publicacion publicacion = guardada(idBorrador);
        assertEquals(List.of("Wifi"), publicacion.getServicios());
        assertEquals(List.of("No fiestas"), publicacion.getReglas());
        assertEquals(List.of("/img/a.jpg"), publicacion.getImagenes());
    }

    // ---------- Decisiones 3 y 4 ----------

    @Test
    @DisplayName("Decisión 3: la publicación de otro anfitrión responde como si no existiera")
    void publicacionDeOtroAnfitrion() {
        NoEncontradoException error = assertThrows(NoEncontradoException.class,
                () -> contenidoPublicacionService.definirServicios(OTRO_ANFITRION, idBorrador, List.of("Wifi")));

        assertEquals("La publicación no existe", error.getMessage());
        assertTrue(guardada(idBorrador).getServicios().isEmpty());
    }

    @Test
    @DisplayName("Decisión 3: una publicación que no existe responde 404")
    void publicacionNoExiste() {
        assertThrows(NoEncontradoException.class,
                () -> contenidoPublicacionService.definirReglas(ANFITRION, 999999L, List.of("No fumar")));
        assertThrows(NoEncontradoException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, 999999L, List.of("/a.jpg")));
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"BORRADOR", "ACTIVA", "PAUSADA"})
    @DisplayName("Decisión 4: se puede cambiar en BORRADOR, ACTIVA y PAUSADA, y el estado no cambia")
    void permiteEstadosEditables(EstadoPublicacion estado) {
        Long id = publicacion(ANFITRION, estado).getId();

        contenidoPublicacionService.definirServicios(ANFITRION, id, List.of("Wifi"));
        contenidoPublicacionService.definirReglas(ANFITRION, id, List.of("No fumar"));
        contenidoPublicacionService.definirImagenes(ANFITRION, id, List.of("/img/a.jpg"));

        Publicacion publicacion = guardada(id);
        assertEquals(estado, publicacion.getEstado());
        assertEquals(List.of("Wifi"), publicacion.getServicios());
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoPublicacion.class, names = {"PENDIENTE_REVISION", "BLOQUEADA"})
    @DisplayName("Decisión 4: en PENDIENTE_REVISION y BLOQUEADA no se puede cambiar (409)")
    void rechazaEstadosNoEditables(EstadoPublicacion estado) {
        Long id = publicacion(ANFITRION, estado).getId();

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> contenidoPublicacionService.definirServicios(ANFITRION, id, List.of("Wifi")));
        assertThrows(ConflictoException.class,
                () -> contenidoPublicacionService.definirReglas(ANFITRION, id, List.of("No fumar")));
        assertThrows(ConflictoException.class,
                () -> contenidoPublicacionService.definirImagenes(ANFITRION, id, List.of("/img/a.jpg")));

        assertEquals("Solo se pueden cambiar los servicios, reglas e imágenes de una publicación en BORRADOR, "
                + "ACTIVA o PAUSADA", error.getMessage());
        assertTrue(guardada(id).getServicios().isEmpty());
    }

    @Test
    @DisplayName("Decisión 4: con datos inválidos se responde 400 aunque la publicación no exista")
    void validaAntesDeBuscar() {
        assertThrows(SolicitudInvalidaException.class,
                () -> contenidoPublicacionService.definirServicios(ANFITRION, 999999L, List.of("Jacuzzi")));
    }
}
