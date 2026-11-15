package co.edu.unbosque.marketplace.resenas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// HU-32: reseña de una estancia completada.
// Decisión 4: una sola reseña por reserva (la restricción única cubre dos solicitudes al mismo tiempo).
@Entity
@Table(name = "resena",
        uniqueConstraints = @UniqueConstraint(name = "uk_resena_reserva", columnNames = "id_reserva"),
        indexes = @Index(name = "idx_resena_publicacion", columnList = "id_publicacion"))
public class Resena {

    public static final int CALIFICACION_MINIMA = 1;
    public static final int CALIFICACION_MAXIMA = 5;
    public static final int LONGITUD_MAXIMA_CONTENIDO = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La reserva, la publicación y el huésped viven en otros servicios; aquí solo se guardan sus ids
    @Column(name = "id_reserva", nullable = false)
    private Long reservaId;

    @Column(name = "id_publicacion", nullable = false)
    private Long publicacionId;

    @Column(name = "id_huesped", nullable = false)
    private Long huespedId;

    @Column(name = "calificacion", nullable = false)
    private int calificacion;

    @Column(name = "contenido", nullable = false, length = LONGITUD_MAXIMA_CONTENIDO)
    private String contenido;

    // Decisión 5: se publica de inmediato. HU-36 la podrá ocultar
    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Resena() {
    }

    public Resena(Long reservaId, Long publicacionId, Long huespedId, int calificacion, String contenido,
            LocalDateTime fecha) {
        this.reservaId = reservaId;
        this.publicacionId = publicacionId;
        this.huespedId = huespedId;
        this.calificacion = calificacion;
        this.contenido = contenido;
        this.visible = true;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public Long getHuespedId() {
        return huespedId;
    }

    public int getCalificacion() {
        return calificacion;
    }

    public String getContenido() {
        return contenido;
    }

    public boolean isVisible() {
        return visible;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
