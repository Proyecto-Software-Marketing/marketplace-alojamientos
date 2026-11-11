package co.edu.unbosque.marketplace.cotizacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Cada vez que el admin cambia el porcentaje se guarda una fila nueva
// La fila más reciente es el porcentaje que se usa
@Entity
@Table(name = "configuracion_tarifa")
public class ConfiguracionTarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    // Quién hizo el cambio
    @Column(name = "id_administrador", nullable = false)
    private Long idAdministrador;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    public ConfiguracionTarifa() {
    }

    public ConfiguracionTarifa(BigDecimal porcentaje, Long idAdministrador) {
        this.porcentaje = porcentaje;
        this.idAdministrador = idAdministrador;
        this.fechaCambio = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public Long getIdAdministrador() {
        return idAdministrador;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }
}