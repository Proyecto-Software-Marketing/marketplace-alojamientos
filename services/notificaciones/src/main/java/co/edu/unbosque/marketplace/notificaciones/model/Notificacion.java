package co.edu.unbosque.marketplace.notificaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// HU-30: notificación registrada para un usuario afectado por un evento.
// CA-084: guarda el tipo de evento, la fecha y la entidad relacionada.
// Decisión 3: un mismo usuario no recibe dos veces el mismo evento de la misma entidad.
@Entity
@Table(name = "notificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacion_evento",
                columnNames = {"id_usuario", "tipo_evento", "id_entidad"}),
        indexes = @Index(name = "idx_notificacion_usuario", columnList = "id_usuario"))
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El usuario vive en el servicio de Identidad; aquí solo se guarda su id
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEvento tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad_tipo", nullable = false, length = 20)
    private TipoEntidad entidadTipo;

    @Column(name = "id_entidad", nullable = false)
    private Long idEntidad;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public Notificacion() {
    }

    public Notificacion(Long idUsuario, TipoEvento tipoEvento, Long idEntidad, String mensaje,
            LocalDateTime fechaCreacion) {
        this.idUsuario = idUsuario;
        this.tipoEvento = tipoEvento;
        this.entidadTipo = tipoEvento.getEntidadTipo();
        this.idEntidad = idEntidad;
        this.mensaje = mensaje;
        this.leida = false;
        this.fechaCreacion = fechaCreacion;
    }

    // CA-086: marcar como leída. Si ya estaba leída no cambia nada
    public void marcarLeida() {
        this.leida = true;
    }

    public Long getId() {
        return id;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public TipoEntidad getEntidadTipo() {
        return entidadTipo;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
