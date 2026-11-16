package co.edu.unbosque.marketplace.alojamientos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

// HU-10 (CA-129): servicio adicional con costo que el huésped puede elegir al cotizar.
// Se cobra una vez por estancia (decisión 11)
@Embeddable
public class ServicioAdicional {

    public static final int LONGITUD_MAXIMA_NOMBRE = 60;

    @Column(name = "nombre", nullable = false, length = LONGITUD_MAXIMA_NOMBRE)
    private String nombre;

    // Pesos sin decimales
    @Column(name = "costo", nullable = false, precision = Publicacion.DIGITOS_MAXIMOS_PRECIO, scale = 0)
    private BigDecimal costo;

    protected ServicioAdicional() {
    }

    public ServicioAdicional(String nombre, BigDecimal costo) {
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
