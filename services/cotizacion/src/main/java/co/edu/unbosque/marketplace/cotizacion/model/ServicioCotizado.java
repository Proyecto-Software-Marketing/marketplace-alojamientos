package co.edu.unbosque.marketplace.cotizacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

// Servicio adicional que eligió el huésped, con el costo que tenía al cotizar (CA-060)
@Embeddable
public class ServicioCotizado {

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "costo", nullable = false, precision = 12, scale = 0)
    private BigDecimal costo;

    protected ServicioCotizado() {
    }

    public ServicioCotizado(String nombre, BigDecimal costo) {
        this.nombre = nombre;
        this.costo = costo;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getCosto() {
        return costo;
    }
}
