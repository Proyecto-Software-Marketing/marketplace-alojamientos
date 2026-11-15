package co.edu.unbosque.marketplace.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas", indexes = {
        @Index(name = "idx_reserva_codigo", columnList = "codigo_reserva"),
        @Index(name = "idx_reserva_publicacion", columnList = "id_publicacion"),
        @Index(name = "idx_reserva_huesped", columnList = "id_huesped"),
        @Index(name = "idx_reserva_idempotencia", columnList = "idempotency_key")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 32)
    private String codigoReserva;

    @Column(name = "id_publicacion", nullable = false)
    private Long publicacionId;

    @Column(name = "id_huesped", nullable = false)
    private Long huespedId;

    @Column(name = "id_cotizacion", nullable = false)
    private String cotizacionId;

    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "cantidad_huespedes", nullable = false)
    private Integer cantidadHuespedes;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "metodo_pago", length = 100)
    private String metodoPago;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private BookingStatus estado;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public Booking() {
    }

    public Booking(String codigoReserva, Long publicacionId, Long huespedId, String cotizacionId,
                   LocalDate fechaLlegada, LocalDate fechaSalida, Integer cantidadHuespedes,
                   BigDecimal montoTotal, String metodoPago, String idempotencyKey,
                   BookingStatus estado, LocalDateTime creadaEn) {
        this.codigoReserva = codigoReserva;
        this.publicacionId = publicacionId;
        this.huespedId = huespedId;
        this.cotizacionId = cotizacionId;
        this.fechaLlegada = fechaLlegada;
        this.fechaSalida = fechaSalida;
        this.cantidadHuespedes = cantidadHuespedes;
        this.montoTotal = montoTotal;
        this.metodoPago = metodoPago;
        this.idempotencyKey = idempotencyKey;
        this.estado = estado;
        this.creadaEn = creadaEn != null ? creadaEn : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public Long getHuespedId() {
        return huespedId;
    }

    public String getCotizacionId() {
        return cotizacionId;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public Integer getCantidadHuespedes() {
        return cantidadHuespedes;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BookingStatus getEstado() {
        return estado;
    }

    public void setEstado(BookingStatus estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
