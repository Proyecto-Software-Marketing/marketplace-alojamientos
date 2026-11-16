package co.edu.unbosque.marketplace.identidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// HU-04 token para recuperar la contraseña, se muestra en una notificación interna
@Entity
@Table(name = "recuperacion_contrasena")
public class RecuperacionContrasena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // Un usuario puede pedir varias recuperaciones, solo la última sirve
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaVencimiento;

    // Queda en true cuando se usa o cuando se pide otra recuperación
    @Column(nullable = false)
    private boolean usado;

    public RecuperacionContrasena() {
    }

    public RecuperacionContrasena(String token, Usuario usuario, LocalDateTime fechaVencimiento) {
        this.token = token;
        this.usuario = usuario;
        this.fechaVencimiento = fechaVencimiento;
        this.usado = false;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }
}
