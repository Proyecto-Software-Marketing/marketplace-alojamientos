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

// HU-16: periodo en el que el anfitrión ofrece su publicación para reservar.
// La fecha de fin funciona como fecha de salida: del 1 al 10 se puede reservar
// con llegada desde el 1 y salida hasta el 10.
@Entity
@Table(name = "periodo_disponible",
        indexes = @Index(name = "idx_periodo_publicacion", columnList = "id_publicacion"))
public class PeriodoDisponible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La publicación vive en el servicio de Alojamientos; aquí solo se guarda su id
    @Column(name = "id_publicacion", nullable = false)
    private Long idPublicacion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public PeriodoDisponible() {
    }

    public PeriodoDisponible(Long idPublicacion, LocalDate fechaInicio, LocalDate fechaFin) {
        this.idPublicacion = idPublicacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.fechaCreacion = LocalDateTime.now();
    }

    // CA-047: las reglas se validan en el servicio antes de llamar a este método
    public void actualizarFechas(LocalDate nuevaFechaInicio, LocalDate nuevaFechaFin) {
        this.fechaInicio = nuevaFechaInicio;
        this.fechaFin = nuevaFechaFin;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getIdPublicacion() {
        return idPublicacion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}