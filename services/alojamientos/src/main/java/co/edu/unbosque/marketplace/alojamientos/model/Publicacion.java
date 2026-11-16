package co.edu.unbosque.marketplace.alojamientos.model;

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

    public Publicacion() {
    }

    // CA-020: queda en BORRADOR y asociada al anfitrión que la crea
    public Publicacion(Long anfitrionId, LocalDateTime creadaEn) {
        this.anfitrionId = anfitrionId;
        this.estado = EstadoPublicacion.BORRADOR;
        this.creadaEn = creadaEn;
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

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
