package co.edu.unbosque.marketplace.cotizacion.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// HU-22: cotización desglosada. Se guarda para que Reservas la consulte con GET /quotes/{id}
// y el huésped reserve con el precio que vio (decisión 2)
@Entity
@Table(name = "cotizacion")
public class Cotizacion {

    // Decisión 2: UUID en texto, porque Reservas maneja el id como String
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "id_huesped", nullable = false)
    private Long idHuesped;

    @Column(name = "id_publicacion", nullable = false)
    private Long publicacionId;

    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "cantidad_huespedes", nullable = false)
    private int cantidadHuespedes;

    @Column(nullable = false)
    private int noches;

    // Todos los valores en pesos sin decimales
    @Column(name = "precio_por_noche", nullable = false, precision = 12, scale = 0)
    private BigDecimal precioPorNoche;

    @Column(name = "precio_base", nullable = false, precision = 14, scale = 0)
    private BigDecimal precioBase;

    @Column(name = "tarifa_limpieza", nullable = false, precision = 12, scale = 0)
    private BigDecimal tarifaLimpieza;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cotizacion_servicio_adicional", joinColumns = @JoinColumn(name = "id_cotizacion"))
    @OrderColumn(name = "posicion")
    private List<ServicioCotizado> serviciosAdicionales = new ArrayList<>();

    @Column(name = "porcentaje_descuento", nullable = false)
    private int porcentajeDescuento;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal descuento;

    @Column(name = "porcentaje_tarifa_servicio", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeTarifaServicio;

    @Column(name = "tarifa_servicio", nullable = false, precision = 14, scale = 0)
    private BigDecimal tarifaServicio;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal total;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    protected Cotizacion() {
    }

    public Cotizacion(String id, Long idHuesped, Long publicacionId, LocalDate fechaLlegada, LocalDate fechaSalida,
            int cantidadHuespedes, LocalDateTime creadaEn) {
        this.id = id;
        this.idHuesped = idHuesped;
        this.publicacionId = publicacionId;
        this.fechaLlegada = fechaLlegada;
        this.fechaSalida = fechaSalida;
        this.cantidadHuespedes = cantidadHuespedes;
        this.creadaEn = creadaEn;
    }

    // CA-060 y CA-061: el desglose ya calculado por CotizacionService
    public void definirDesglose(int noches, BigDecimal precioPorNoche, BigDecimal precioBase,
            BigDecimal tarifaLimpieza, List<ServicioCotizado> serviciosAdicionales, int porcentajeDescuento,
            BigDecimal descuento, BigDecimal porcentajeTarifaServicio, BigDecimal tarifaServicio, BigDecimal total) {
        this.noches = noches;
        this.precioPorNoche = precioPorNoche;
        this.precioBase = precioBase;
        this.tarifaLimpieza = tarifaLimpieza;
        this.serviciosAdicionales.clear();
        this.serviciosAdicionales.addAll(serviciosAdicionales);
        this.porcentajeDescuento = porcentajeDescuento;
        this.descuento = descuento;
        this.porcentajeTarifaServicio = porcentajeTarifaServicio;
        this.tarifaServicio = tarifaServicio;
        this.total = total;
    }

    public String getId() {
        return id;
    }

    public Long getIdHuesped() {
        return idHuesped;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public int getCantidadHuespedes() {
        return cantidadHuespedes;
    }

    public int getNoches() {
        return noches;
    }

    public BigDecimal getPrecioPorNoche() {
        return precioPorNoche;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public BigDecimal getTarifaLimpieza() {
        return tarifaLimpieza;
    }

    public List<ServicioCotizado> getServiciosAdicionales() {
        return List.copyOf(serviciosAdicionales);
    }

    public int getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public BigDecimal getPorcentajeTarifaServicio() {
        return porcentajeTarifaServicio;
    }

    public BigDecimal getTarifaServicio() {
        return tarifaServicio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
