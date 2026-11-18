package co.edu.unbosque.marketplace.alojamientos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "politica_cancelacion")
public class PoliticaCancelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "porcentaje_devolucion", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal porcentajeDevolucion;

    @Column(name = "permite_liberacion_periodo", nullable = false)
    private boolean permiteLiberacionPeriodo;

    protected PoliticaCancelacion() {
    }

    public PoliticaCancelacion(String nombre, String descripcion,
            BigDecimal porcentajeDevolucion,
            boolean permiteLiberacionPeriodo) {
        if (nombre == null || nombre.isBlank()
                || descripcion == null || descripcion.isBlank()
                || porcentajeDevolucion == null
                || porcentajeDevolucion.compareTo(BigDecimal.ZERO) < 0
                || porcentajeDevolucion.compareTo(
                        new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Los datos de la politica no son validos");
        }

        this.nombre = nombre;
        this.descripcion = descripcion;
        this.porcentajeDevolucion = porcentajeDevolucion;
        this.permiteLiberacionPeriodo = permiteLiberacionPeriodo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPorcentajeDevolucion() {
        return porcentajeDevolucion;
    }

    public boolean isPermiteLiberacionPeriodo() {
        return permiteLiberacionPeriodo;
    }
}
