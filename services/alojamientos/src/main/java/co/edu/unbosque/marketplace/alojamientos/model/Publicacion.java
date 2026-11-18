package co.edu.unbosque.marketplace.alojamientos.model;

import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


// HU-07: publicación de un alojamiento. La crea un anfitrión y nace en BORRADOR (CA-020)
@Entity
@Table(name = "publicacion", indexes = {
        @Index(name = "idx_publicacion_anfitrion", columnList = "id_anfitrion"),
        @Index(name = "idx_publicacion_estado", columnList = "estado")})
public class Publicacion {

    // Decisiones 6 y 8: límites de los datos básicos
    public static final int LONGITUD_MAXIMA_TITULO = 120;
    public static final int LONGITUD_MAXIMA_DESCRIPCION = 2000;
    public static final int LONGITUD_MAXIMA_CIUDAD = 80;
    public static final int LONGITUD_MAXIMA_UBICACION = 200;
    public static final int CAPACIDAD_MINIMA = 1;
    public static final int CAPACIDAD_MAXIMA = 50;
    public static final int DIGITOS_MAXIMOS_PRECIO = 9;

    // HU-09, decisiones 8 y 10: límites de reglas e imágenes
    public static final int CANTIDAD_MAXIMA_REGLAS = 20;
    public static final int LONGITUD_MAXIMA_REGLA = 200;
    public static final int CANTIDAD_MAXIMA_IMAGENES = 10;
    public static final int LONGITUD_MAXIMA_IMAGEN = 500;

    // HU-10, decisiones 4 a 6: límites de las tarifas
    public static final int PORCENTAJE_MAXIMO_DESCUENTO = 90;
    public static final int NOCHES_MINIMAS_DESCUENTO = 2;
    public static final int NOCHES_MAXIMAS_DESCUENTO = 365;
    public static final int CANTIDAD_MAXIMA_SERVICIOS_ADICIONALES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El anfitrión vive en el servicio de Identidad; aquí solo se guarda su id
    @Column(name = "id_anfitrion", nullable = false)
    private Long anfitrionId;

    @Column(name = "titulo", nullable = false, length = LONGITUD_MAXIMA_TITULO)
    private String titulo;

    @Column(name = "descripcion", nullable = false, length = LONGITUD_MAXIMA_DESCRIPCION)
    private String descripcion;

    @Column(name = "ciudad", nullable = false, length = LONGITUD_MAXIMA_CIUDAD)
    private String ciudad;

    @Column(name = "ubicacion_textual", nullable = false, length = LONGITUD_MAXIMA_UBICACION)
    private String ubicacionTextual;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAlojamiento tipo;

    @Column(name = "capacidad", nullable = false)
    private int capacidad;

    // Pesos sin decimales
    @Column(name = "precio_noche", nullable = false, precision = DIGITOS_MAXIMOS_PRECIO, scale = 0)
    private BigDecimal precioNoche;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPublicacion estado;
// HU-13: motivo que indica el administrador al rechazar la publicacion.
@Column(name = "motivo_rechazo", length = 2000)
private String motivoRechazo;
    @Column(name = "motivo_bloqueo", length = 2000)
    private String motivoBloqueo;
    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    // HU-09 (CA-026): servicios del catálogo, en el orden del catálogo.
    // Se cargan siempre con la publicación porque todas las respuestas los muestran
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "publicacion_servicio", joinColumns = @JoinColumn(name = "id_publicacion"))
    @OrderColumn(name = "posicion")
    @Column(name = "servicio", nullable = false, length = 40)
    private List<String> servicios = new ArrayList<>();

    // HU-09 (CA-027): reglas del alojamiento, en el orden en que las escribió el anfitrión
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "publicacion_regla", joinColumns = @JoinColumn(name = "id_publicacion"))
    @OrderColumn(name = "posicion")
    @Column(name = "regla", nullable = false, length = LONGITUD_MAXIMA_REGLA)
    private List<String> reglas = new ArrayList<>();

    // HU-09 (CA-028): rutas o URL de imágenes; la primera es la foto principal
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "publicacion_imagen", joinColumns = @JoinColumn(name = "id_publicacion"))
    @OrderColumn(name = "posicion")
    @Column(name = "imagen", nullable = false, length = LONGITUD_MAXIMA_IMAGEN)
    private List<String> imagenes = new ArrayList<>();

    // HU-10 (CA-029): tarifa de limpieza en pesos, se cobra una vez por estancia. 0 = sin limpieza
    @Column(name = "tarifa_limpieza", nullable = false, precision = DIGITOS_MAXIMOS_PRECIO, scale = 0)
    private BigDecimal tarifaLimpieza = BigDecimal.ZERO;

    // HU-10 (CA-030): porcentaje de descuento por estancia larga. 0 = sin descuento
    @Column(name = "porcentaje_descuento", nullable = false)
    private int porcentajeDescuento;

    // HU-10 (CA-030): noches desde las que aplica el descuento; null si no hay descuento
    @Column(name = "noches_minimas_descuento")
    private Integer nochesMinimasDescuento;

    // HU-10 (CA-129): servicios adicionales con costo, en el orden en que los escribió el anfitrión
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "publicacion_servicio_adicional", joinColumns = @JoinColumn(name = "id_publicacion"))
    @OrderColumn(name = "posicion")
    private List<ServicioAdicional> serviciosAdicionales = new ArrayList<>();

    // HU-11: política de cancelación asociada a la publicación.
    // Puede ser null mientras la publicación esté en borrador.
    @jakarta.persistence.ManyToOne
    @JoinColumn(name = "politica_cancelacion_id")
    private PoliticaCancelacion politicaCancelacion;

    public PoliticaCancelacion getPoliticaCancelacion() {
        return politicaCancelacion;
    }

    public void definirPoliticaCancelacion(PoliticaCancelacion politica) {
        this.politicaCancelacion = politica;
    }
    public Publicacion() {
    }

    // CA-020: queda en BORRADOR y asociada al anfitrión que la crea.
    // HU-10, decisión 8: sin limpieza, sin descuento y sin servicios adicionales
    public Publicacion(Long anfitrionId, LocalDateTime creadaEn) {
        this.anfitrionId = anfitrionId;
        this.estado = EstadoPublicacion.BORRADOR;
        this.creadaEn = creadaEn;
    }

    // HU-10, decisión 1: la configuración de precios se reemplaza completa
    public void definirTarifas(BigDecimal tarifaLimpieza, int porcentajeDescuento, Integer nochesMinimasDescuento,
            List<ServicioAdicional> serviciosAdicionales) {
        this.tarifaLimpieza = tarifaLimpieza;
        this.porcentajeDescuento = porcentajeDescuento;
        this.nochesMinimasDescuento = nochesMinimasDescuento;
        this.serviciosAdicionales.clear();
        this.serviciosAdicionales.addAll(serviciosAdicionales);
    }

    public BigDecimal getTarifaLimpieza() {
        return tarifaLimpieza;
    }

    public int getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public Integer getNochesMinimasDescuento() {
        return nochesMinimasDescuento;
    }

    public List<ServicioAdicional> getServiciosAdicionales() {
        return List.copyOf(serviciosAdicionales);
    }

    // Datos básicos de la HU-07 (los mismos que editará la HU-08)
    public void definirDatosBasicos(String titulo, String descripcion, String ciudad, String ubicacionTextual,
            TipoAlojamiento tipo, int capacidad, BigDecimal precioNoche) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.ubicacionTextual = ubicacionTextual;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.precioNoche = precioNoche;
    }

    // Decisión 15: al desactivar la cuenta del anfitrión sus publicaciones ACTIVA pasan a PAUSADA.
    // En cualquier otro estado no cambia nada
    public void pausar() {
        if (estado == EstadoPublicacion.ACTIVA) {
            estado = EstadoPublicacion.PAUSADA;
        }
    }
// HU-14 (CA-041 y CA-042): reactivar una publicacion pausada.
// Las publicaciones bloqueadas no pueden ser reactivadas.
public void reactivar() {
    if (estado != EstadoPublicacion.PAUSADA) {
        throw new IllegalStateException(
                "Solo se pueden reactivar publicaciones en estado PAUSADA");
    }

    estado = EstadoPublicacion.ACTIVA;
}
    // HU-09, decisión 4: los servicios, reglas e imágenes se pueden cambiar en BORRADOR, ACTIVA y PAUSADA.
    // En PENDIENTE_REVISION el administrador la está revisando y BLOQUEADA no se puede tocar
    public boolean permiteEditarContenido() {
        return estado == EstadoPublicacion.BORRADOR
                || estado == EstadoPublicacion.ACTIVA
                || estado == EstadoPublicacion.PAUSADA;
    }

    // HU-09, decisión 2: cada lista reemplaza por completo a la anterior
    public void definirServicios(List<String> nuevos) {
        servicios.clear();
        servicios.addAll(nuevos);
    }

    public void definirReglas(List<String> nuevas) {
        reglas.clear();
        reglas.addAll(nuevas);
    }

    public void definirImagenes(List<String> nuevas) {
        imagenes.clear();
        imagenes.addAll(nuevas);
    }

    public List<String> getServicios() {
        return List.copyOf(servicios);
    }

    public List<String> getReglas() {
        return List.copyOf(reglas);
    }

    public List<String> getImagenes() {
        return List.copyOf(imagenes);
    }

    public Long getId() {
        return id;
    }

    public Long getAnfitrionId() {
        return anfitrionId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getUbicacionTextual() {
        return ubicacionTextual;
    }

    public TipoAlojamiento getTipo() {
        return tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    // HU-12 (CA-035): enviar una publicacion en borrador a revision.
public void enviarARevision() {
    if (estado != EstadoPublicacion.BORRADOR) {
        throw new IllegalStateException(
                "Solo se pueden enviar a revision publicaciones en BORRADOR");
    }

    estado = EstadoPublicacion.PENDIENTE_REVISION;
    motivoRechazo = null;
}
    public EstadoPublicacion getEstado() {
        return estado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
    public String getMotivoRechazo() {
    return motivoRechazo;
}

    // HU-13 (CA-037): aprobar una publicacion pendiente de revision.
    public void aprobar() {
        if (estado != EstadoPublicacion.PENDIENTE_REVISION) {
            throw new IllegalStateException(
                    "Solo se pueden aprobar publicaciones en PENDIENTE_REVISION");
        }

        estado = EstadoPublicacion.ACTIVA;
        motivoRechazo = null;
    }
// HU-13 (CA-038): rechazar una publicacion indicando el motivo.
public void rechazar(String motivo) {
    if (estado != EstadoPublicacion.PENDIENTE_REVISION) {
        throw new IllegalStateException(
                "Solo se pueden rechazar publicaciones en PENDIENTE_REVISION");
    }

    if (motivo == null || motivo.isBlank()
            || motivo.trim().length() < 3
            || motivo.trim().length() > 2000) {
        throw new SolicitudInvalidaException(
        "El motivo del rechazo debe tener entre 3 y 2000 caracteres");
    }

    estado = EstadoPublicacion.BORRADOR;
    motivoRechazo = motivo.trim();
}
public void bloquear(String motivo) {
        if (estado == EstadoPublicacion.BLOQUEADA) {
            throw new IllegalStateException("La publicacion ya esta bloqueada");
        }
        this.motivoBloqueo = motivo;
        this.estado = EstadoPublicacion.BLOQUEADA;
    }

    public String getMotivoBloqueo() {
        return motivoBloqueo;
    }
}
