package co.edu.unbosque.marketplace.disponibilidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Fechas ocupadas por una reserva confirmada.
// La crea Reservas al confirmar (HU-18) y se usa para no eliminar
// ni recortar periodos con reservas (HU-16, CA-047).
@Entity
@Table(name = "bloqueo_reserva",
        indexes = @Index(name = "idx_bloqueo_publicacion", columnList = "id_publicacion"))
public class BloqueoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_publicacion", nullable = false)
    private Long idPublicacion;

    // Único: si Reservas reintenta bloquear la misma reserva no se duplica
    @Column(name = "id_reserva", nullable = false, unique = true)
    private Long idReserva;

    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public BloqueoReserva() {
    }

    public BloqueoReserva(Long idPublicacion, Long idReserva, LocalDate fechaLlegada, LocalDate fechaSalida) {
        this.idPublicacion = idPublicacion;
        this.idReserva = idReserva;
        this.fechaLlegada = fechaLlegada;
        this.fechaSalida = fechaSalida;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getIdPublicacion() {
        return idPublicacion;
    }

    public Long getIdReserva() {
        return idReserva;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}