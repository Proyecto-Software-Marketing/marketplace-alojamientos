package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import co.edu.unbosque.marketplace.alojamientos.exception.PublicacionIncompletaException;
import org.springframework.test.util.ReflectionTestUtils;

// Pruebas de la HU-07: crear publicación en borrador
@SpringBootTest
class PublicacionServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 30);
    private static final Long ANFITRION = 7L;

    @Autowired
    private PublicacionService publicacionService;

    @Autowired
    private PublicacionRepository publicacionRepository;

    // Reloj simulado para comprobar la fecha de creación
    @MockitoBean
    private Clock reloj;

    @BeforeEach
    void preparar() {
        publicacionRepository.deleteAll();
        when(reloj.getZone()).thenReturn(BOGOTA);
        when(reloj.instant()).thenReturn(AHORA.atZone(BOGOTA).toInstant());
    }
    private void completar(Publicacion publicacion) {
        publicacion.definirServicios(java.util.List.of("Wifi"));
        publicacion.definirReglas(java.util.List.of("No fumar"));
        publicacion.definirImagenes(java.util.List.of("https://ejemplo.com/foto.jpg"));
        publicacionRepository.save(publicacion);
    }

    // Solicitud válida a la que cada prueba le cambia la capacidad y el precio
    private PublicacionRequestDTO solicitud(String capacidad, String precio) {
        return solicitud("CABANA", capacidad, precio);
    }

    private PublicacionRequestDTO solicitud(String tipo, String capacidad, String precio) {
        return new PublicacionRequestDTO("Cabaña en el lago", "Cabaña de madera con vista al lago",
                "Guatavita", "Vereda Tominé, km 3", tipo, new BigDecimal(capacidad), new BigDecimal(precio));
    }

    // ---------- CA-020 ----------

    @Test
    @DisplayName("CA-020 la publicación queda en BORRADOR asociada al anfitrión que la crea")
    void creaPublicacionEnBorrador() {
        // Dado que soy el anfitrión 7
        // Cuando creo una publicación con título, descripción, ubicación, tipo, capacidad y precio
        Publicacion publicacion = publicacionService.crear(ANFITRION, solicitud("4", "250000"));

        // Entonces queda en BORRADOR asociada a mí
        assertNotNull(publicacion.getId());
        assertEquals(EstadoPublicacion.BORRADOR, publicacion.getEstado());
        assertEquals(ANFITRION, publicacion.getAnfitrionId());
        assertEquals("Cabaña en el lago", publicacion.getTitulo());
        assertEquals("Cabaña de madera con vista al lago", publicacion.getDescripcion());
        assertEquals("Guatavita", publicacion.getCiudad());
        assertEquals("Vereda Tominé, km 3", publicacion.getUbicacionTextual());
        assertEquals(TipoAlojamiento.CABANA, publicacion.getTipo());
        assertEquals(4, publicacion.getCapacidad());
        assertEquals(new BigDecimal("250000"), publicacion.getPrecioNoche());
        assertEquals(AHORA, publicacion.getCreadaEn());
        assertEquals(1, publicacionRepository.count());
    }

    @Test
    @DisplayName("CA-020 la publicación guardada conserva el estado y el anfitrión al consultarla")
    void guardaPublicacion() {
        Long id = publicacionService.crear(ANFITRION, solicitud("4", "250000")).getId();

        Publicacion guardada = publicacionRepository.findById(id).orElseThrow();
        assertEquals(EstadoPublicacion.BORRADOR, guardada.getEstado());
        assertEquals(ANFITRION, guardada.getAnfitrionId());
        assertEquals(0, new BigDecimal("250000").compareTo(guardada.getPrecioNoche()));
    }

    @Test
    @DisplayName("CA-020 se quitan los espacios sobrantes de los textos")
    void quitaEspacios() {
        PublicacionRequestDTO conEspacios = new PublicacionRequestDTO("  Cabaña en el lago ", " Con vista  ",
                "  Guatavita ", " Vereda Tominé ", "CABANA", BigDecimal.valueOf(4), BigDecimal.valueOf(250000));

        Publicacion publicacion = publicacionService.crear(ANFITRION, conEspacios);

        assertEquals("Cabaña en el lago", publicacion.getTitulo());
        assertEquals("Con vista", publicacion.getDescripcion());
        assertEquals("Guatavita", publicacion.getCiudad());
        assertEquals("Vereda Tominé", publicacion.getUbicacionTextual());
    }

    @ParameterizedTest(name = "tipo {0}")
    @EnumSource(TipoAlojamiento.class)
    @DisplayName("Decisión 7: se aceptan todos los tipos de alojamiento del contrato")
    void aceptaTodosLosTipos(TipoAlojamiento tipo) {
        Publicacion publicacion = publicacionService.crear(ANFITRION, solicitud(tipo.name(), "2", "90000"));

        assertEquals(tipo, publicacion.getTipo());
    }

    @Test
    @DisplayName("Decisión 8: un precio escrito con exponente se guarda como entero en pesos")
    void guardaPrecioSinExponente() {
        Publicacion publicacion = publicacionService.crear(ANFITRION, solicitud("2", "1.5E+5"));

        assertEquals("150000", publicacion.getPrecioNoche().toPlainString());
        assertEquals(0, publicacion.getPrecioNoche().scale());
    }

    @Test
    @DisplayName("Decisión 9: el mismo anfitrión puede crear dos publicaciones con el mismo título")
    void permiteTituloRepetido() {
        Publicacion primera = publicacionService.crear(ANFITRION, solicitud("4", "250000"));
        Publicacion segunda = publicacionService.crear(ANFITRION, solicitud("4", "250000"));

        assertNotEquals(primera.getId(), segunda.getId());
        assertEquals(2, publicacionRepository.count());
    }

    // ---------- CA-021 ----------

    @ParameterizedTest(name = "capacidad {0}")
    @ValueSource(strings = {"0", "-1", "-20"})
    @DisplayName("CA-021 una capacidad menor o igual a cero se rechaza y no se guarda nada")
    void rechazaCapacidadNoPositiva(String capacidad) {
        // Dado que ingreso una capacidad menor o igual a cero
        PublicacionRequestDTO invalida = solicitud(capacidad, "250000");

        // Cuando guardo, entonces se rechaza indicando el error
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> publicacionService.crear(ANFITRION, invalida));
        assertEquals("La capacidad debe ser un número entero entre 1 y 50", error.getMessage());
        assertEquals(0, publicacionRepository.count());
    }

    @ParameterizedTest(name = "precio {0}")
    @ValueSource(strings = {"0", "-1", "-250000"})
    @DisplayName("CA-021 un precio por noche que no es positivo se rechaza y no se guarda nada")
    void rechazaPrecioNoPositivo(String precio) {
        // Dado que ingreso un precio por noche que no es positivo
        PublicacionRequestDTO invalida = solicitud("4", precio);

        // Cuando guardo, entonces se rechaza indicando el error
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> publicacionService.crear(ANFITRION, invalida));
        assertEquals(PublicacionRequestDTO.MENSAJE_PRECIO, error.getMessage());
        assertEquals(0, publicacionRepository.count());
    }

    @Test
    @DisplayName("CA-021 sin capacidad o sin precio la publicación se rechaza")
    void rechazaSinCapacidadOPrecio() {
        PublicacionRequestDTO sinCapacidad = new PublicacionRequestDTO("Cabaña", "Con vista", "Guatavita",
                "Vereda Tominé", "CABANA", null, BigDecimal.valueOf(250000));
        PublicacionRequestDTO sinPrecio = new PublicacionRequestDTO("Cabaña", "Con vista", "Guatavita",
                "Vereda Tominé", "CABANA", BigDecimal.valueOf(4), null);

        assertThrows(SolicitudInvalidaException.class, () -> publicacionService.crear(ANFITRION, sinCapacidad));
        assertThrows(SolicitudInvalidaException.class, () -> publicacionService.crear(ANFITRION, sinPrecio));
        assertEquals(0, publicacionRepository.count());
    }

    @ParameterizedTest(name = "capacidad {0}")
    @ValueSource(strings = {"1", "50"})
    @DisplayName("Decisión 8: se aceptan los extremos de la capacidad, 1 y 50")
    void aceptaExtremosDeCapacidad(String capacidad) {
        Publicacion publicacion = publicacionService.crear(ANFITRION, solicitud(capacidad, "1"));

        assertEquals(Integer.parseInt(capacidad), publicacion.getCapacidad());
    }

// ---------- HU-12: CA-035 y CA-036 ----------

@Test
@DisplayName("CA-035: una publicacion completa pasa a PENDIENTE_REVISION")
void enviaPublicacionCompletaARevision() {
    Publicacion borrador = publicacionService.crear(
            ANFITRION, solicitud("4", "250000"));
            completar(borrador);

    Publicacion enviada = publicacionService.enviarARevision(
            borrador.getId(), ANFITRION);

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION, enviada.getEstado());

    Publicacion guardada = publicacionRepository.findById(
            borrador.getId()).orElseThrow();

    assertEquals(EstadoPublicacion.PENDIENTE_REVISION, guardada.getEstado());
}

@Test
@DisplayName("CA-036: un borrador incompleto indica los datos faltantes")
void rechazaPublicacionIncompleta() {
    Publicacion incompleta = new Publicacion(ANFITRION, AHORA);
    incompleta.definirDatosBasicos(
            "", "", "Guatavita", "", TipoAlojamiento.CABANA,
            4, BigDecimal.valueOf(250000));

    incompleta = publicacionRepository.save(incompleta);
    Long id = incompleta.getId();

    PublicacionIncompletaException error = assertThrows(
        PublicacionIncompletaException.class,
        () -> publicacionService.enviarARevision(id, ANFITRION));

    assertTrue(error.getMessage().contains("titulo"));
    assertTrue(error.getMessage().contains("descripcion"));
    assertTrue(error.getMessage().contains("ubicacion"));

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-12: otro anfitrion no puede enviar la publicacion")
void rechazaPublicacionDeOtroAnfitrion() {
    Publicacion borrador = publicacionService.crear(
            ANFITRION, solicitud("4", "250000"));

    assertThrows(PermisoException.class,
            () -> publicacionService.enviarARevision(
                    borrador.getId(), 99L));

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(
                    borrador.getId()).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-12: no permite enviar dos veces a revision")
void rechazaSegundoEnvioARevision() {
    Publicacion borrador = publicacionService.crear(
            ANFITRION, solicitud("4", "250000"));
            completar(borrador);

    publicacionService.enviarARevision(borrador.getId(), ANFITRION);

    assertThrows(ConflictoException.class,
            () -> publicacionService.enviarARevision(
                    borrador.getId(), ANFITRION));
}

@Test
@DisplayName("HU-12: una publicacion inexistente devuelve error")
void rechazaPublicacionInexistente() {
    assertThrows(NoEncontradoException.class,
            () -> publicacionService.enviarARevision(
                    999999L, ANFITRION));
}

@Test
@DisplayName("HU-12: identifica todos los datos faltantes")
void identificaTodosLosDatosFaltantes() {
    Publicacion incompleta = new Publicacion(ANFITRION, AHORA);

    incompleta.definirDatosBasicos(
        "", "", "", "", TipoAlojamiento.CASA,
        0, BigDecimal.ZERO);

    Long id = publicacionRepository.save(incompleta).getId();

    PublicacionIncompletaException error = assertThrows(
            PublicacionIncompletaException.class,
            () -> publicacionService.enviarARevision(id, ANFITRION));

    assertTrue(error.getMessage().contains("titulo"));
    assertTrue(error.getMessage().contains("descripcion"));
    assertTrue(error.getMessage().contains("ciudad"));
    assertTrue(error.getMessage().contains("ubicacion"));
    assertTrue(error.getMessage().contains("capacidad"));

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-12: rechaza capacidad superior a 50 y precio negativo")
void rechazaCapacidadYPrecioInvalidosEnRevision() {
    Publicacion incompleta = new Publicacion(ANFITRION, AHORA);

    incompleta.definirDatosBasicos(
            "Casa campestre",
            "Casa con piscina",
            "Bogota",
            "Zona norte",
            TipoAlojamiento.CASA,
            51,
            BigDecimal.valueOf(-1000));

    Long id = publicacionRepository.save(incompleta).getId();

    PublicacionIncompletaException error = assertThrows(
            PublicacionIncompletaException.class,
            () -> publicacionService.enviarARevision(id, ANFITRION));

    assertTrue(error.getMessage().contains("capacidad"));
    assertTrue(error.getMessage().contains("precio por noche"));

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}
// HU-14 (CA-040): pausar una publicacion activa.
@Test
@DisplayName("CA-040: una publicacion ACTIVA pasa a PAUSADA")
void pausaPublicacionActiva() {
    Publicacion publicacion = new Publicacion(ANFITRION, AHORA);
    publicacion.definirDatosBasicos(
        "Casa campestre",
        "Casa con piscina",
        "Bogota",
        "Zona norte",
        TipoAlojamiento.CASA,
        4,
        BigDecimal.valueOf(250000));

    publicacion.enviarARevision();
    publicacion.aprobar();

    Long id = publicacionRepository.save(publicacion).getId();

    Publicacion pausada = publicacionService.pausar(id, ANFITRION);

    assertEquals(EstadoPublicacion.PAUSADA, pausada.getEstado());

    assertEquals(EstadoPublicacion.PAUSADA,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

// HU-14 (CA-041): reactivar una publicacion pausada.
// HU-14 (CA-041): reactivar una publicacion pausada.
@Test
@DisplayName("CA-041: una publicacion PAUSADA vuelve a ACTIVA")
void reactivaPublicacionPausada() {
    Publicacion publicacion = new Publicacion(ANFITRION, AHORA);

    publicacion.definirDatosBasicos(
            "Casa campestre",
            "Casa con piscina",
            "Bogota",
            "Zona norte",
            TipoAlojamiento.CASA,
            4,
            BigDecimal.valueOf(250000));

    publicacion.enviarARevision();
    publicacion.aprobar();
    publicacion.pausar();

    Long id = publicacionRepository.save(publicacion).getId();

    Publicacion reactivada = publicacionService.reactivar(id, ANFITRION);

    assertEquals(EstadoPublicacion.ACTIVA, reactivada.getEstado());

    assertEquals(EstadoPublicacion.ACTIVA,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}
// HU-14 (CA-042): una publicacion bloqueada no puede reactivarse.
@Test
@DisplayName("CA-042: no permite reactivar una publicacion BLOQUEADA")
void noReactivaPublicacionBloqueada() {
    Publicacion publicacion = new Publicacion(ANFITRION, AHORA);

    publicacion.definirDatosBasicos(
            "Casa campestre",
            "Casa con piscina",
            "Bogota",
            "Zona norte",
            TipoAlojamiento.CASA,
            4,
            BigDecimal.valueOf(250000));

    ReflectionTestUtils.setField(
            publicacion, "estado", EstadoPublicacion.BLOQUEADA);

    Long id = publicacionRepository.save(publicacion).getId();

    assertThrows(ConflictoException.class,
            () -> publicacionService.reactivar(id, ANFITRION));

    assertEquals(EstadoPublicacion.BLOQUEADA,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-14: otro anfitrion no puede pausar una publicacion")
void noPermitePausarPublicacionDeOtroAnfitrion() {
    Publicacion publicacion = new Publicacion(ANFITRION, AHORA);

    publicacion.definirDatosBasicos(
            "Casa campestre",
            "Casa con piscina",
            "Bogota",
            "Zona norte",
            TipoAlojamiento.CASA,
            4,
            BigDecimal.valueOf(250000));

    publicacion.enviarARevision();
    publicacion.aprobar();

    Long id = publicacionRepository.save(publicacion).getId();

    assertThrows(PermisoException.class,
            () -> publicacionService.pausar(id, 99L));

    assertEquals(EstadoPublicacion.ACTIVA,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-14: otro anfitrion no puede reactivar una publicacion")
void noPermiteReactivarPublicacionDeOtroAnfitrion() {
    Publicacion publicacion = new Publicacion(ANFITRION, AHORA);

    publicacion.definirDatosBasicos(
            "Casa campestre",
            "Casa con piscina",
            "Bogota",
            "Zona norte",
            TipoAlojamiento.CASA,
            4,
            BigDecimal.valueOf(250000));

    publicacion.enviarARevision();
    publicacion.aprobar();
    publicacion.pausar();

    Long id = publicacionRepository.save(publicacion).getId();

    assertThrows(PermisoException.class,
            () -> publicacionService.reactivar(id, 99L));

    assertEquals(EstadoPublicacion.PAUSADA,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-14: no permite pausar una publicacion en BORRADOR")
void noPermitePausarPublicacionEnBorrador() {
    Publicacion publicacion = new Publicacion(ANFITRION, AHORA);

    publicacion.definirDatosBasicos(
            "Casa campestre",
            "Casa con piscina",
            "Bogota",
            "Zona norte",
            TipoAlojamiento.CASA,
            4,
            BigDecimal.valueOf(250000));

    Long id = publicacionRepository.save(publicacion).getId();

    assertThrows(ConflictoException.class,
            () -> publicacionService.pausar(id, ANFITRION));

    assertEquals(EstadoPublicacion.BORRADOR,
            publicacionRepository.findById(id).orElseThrow().getEstado());
}

@Test
@DisplayName("HU-12: sin servicios, reglas ni imagenes no se puede enviar a revision")
void exigeServiciosReglasEImagenes() {
    Publicacion borrador = publicacionService.crear(
            ANFITRION, solicitud("4", "250000"));
    Long id = borrador.getId();

    PublicacionIncompletaException error = assertThrows(
            PublicacionIncompletaException.class,
            () -> publicacionService.enviarARevision(id, ANFITRION));

    assertTrue(error.getMessage().contains("servicios"));
    assertTrue(error.getMessage().contains("reglas"));
    assertTrue(error.getMessage().contains("imagenes"));
}
}
