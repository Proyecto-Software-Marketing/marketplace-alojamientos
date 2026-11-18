package co.edu.unbosque.marketplace.pagos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_payment_idempotency",
            columnNames = "idempotency_key"
        )
    }
)
public class Payment {

    @Id
    private UUID id;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(name = "metodo_pago", length = 100)
    private String metodoPago;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String claveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus estado;

    protected Payment() {
    }

    public Payment(
            UUID id,
            String stripePaymentIntentId,
            BigDecimal monto,
            String moneda,
            String claveIdempotencia,
            PaymentStatus estado) {

        this.id = id;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.monto = monto;
        this.moneda = moneda;
        this.claveIdempotencia = claveIdempotencia;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public String getMetodoPago() {
        return metodoPago;
    }
    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public PaymentStatus getEstado() {
        return estado;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public void setEstado(PaymentStatus estado) {
        this.estado = estado;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }
}