package co.edu.unbosque.marketplace.alojamientos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
