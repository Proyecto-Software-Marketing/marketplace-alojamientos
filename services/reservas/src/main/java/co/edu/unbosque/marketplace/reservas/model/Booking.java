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

// HU-23: reserva inmediata confirmada a partir de una cotización.
// Los identificadores de usuario y publicación viven en sus propios servicios;
// aquí solo se guardan sus ids.
@Entity
@Table(name = "reserva", indexes = {
        @Index(name = "idx_reserva_publicacion", columnList = "id_publicacion"),
        @Index(name = "idx_reserva_huesped", columnList = "id_huesped"),
        @Index(name = "idx_reserva_anfitrion", columnList = "id_anfitrion"),
        @Index(name = "idx_reserva_codigo", columnList = "codigo_reserva", unique = true)
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

    // HU-26: dueño de la publicación, para que el anfitrión consulte sus reservas
    @Column(name = "id_anfitrion")
    private Long anfitrionId;

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

    @Column(name = "metodo_pago", nullable = false, length = 100)
    private String metodoPago;

    // MKT-41: referencias del pago para conciliacion.
    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private BookingStatus estado;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    // HU-28: datos de la cancelación y devolución simulada
    @Column(name = "id_cancelada_por")
    private Long canceladaPor;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "valor_devolucion_simulada", precision = 12, scale = 2)
    private BigDecimal valorDevolucionSimulada;

    public Booking() {
    }

    public Booking(String codigoReserva, Long publicacionId, Long huespedId, Long anfitrionId,
                   String cotizacionId, LocalDate fechaLlegada, LocalDate fechaSalida,
                   Integer cantidadHuespedes, BigDecimal montoTotal, String metodoPago,
                   String idempotencyKey, BookingStatus estado, LocalDateTime creadaEn) {
        this.codigoReserva = codigoReserva;
        this.publicacionId = publicacionId;
        this.huespedId = huespedId;
        this.anfitrionId = anfitrionId;
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

    public Long getAnfitrionId() {
        return anfitrionId;
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

    public String getPaymentId() {
        return paymentId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void registrarPago(String paymentId, String stripePaymentIntentId) {
        this.paymentId = paymentId;
        this.stripePaymentIntentId = stripePaymentIntentId;
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

    public Long getCanceladaPor() {
        return canceladaPor;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public BigDecimal getValorDevolucionSimulada() {
        return valorDevolucionSimulada;
    }

    // HU-28: deja la reserva CANCELADA con el detalle de la devolución simulada
    public void cancelar(Long canceladaPor, String motivo, LocalDateTime fechaCancelacion,
            BigDecimal valorDevolucionSimulada) {
        this.estado = BookingStatus.CANCELADA;
        this.canceladaPor = canceladaPor;
        this.motivoCancelacion = motivo;
        this.fechaCancelacion = fechaCancelacion;
        this.valorDevolucionSimulada = valorDevolucionSimulada;
    }
}
