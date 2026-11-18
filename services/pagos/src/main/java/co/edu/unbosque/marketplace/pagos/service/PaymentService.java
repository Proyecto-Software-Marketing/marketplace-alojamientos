package co.edu.unbosque.marketplace.pagos.service;

import co.edu.unbosque.marketplace.pagos.dto.ChargeRequestDTO;
import co.edu.unbosque.marketplace.pagos.dto.PaymentResponseDTO;
import co.edu.unbosque.marketplace.pagos.exception.PagoRechazadoException;
import co.edu.unbosque.marketplace.pagos.exception.ServicioPagoException;
import co.edu.unbosque.marketplace.pagos.model.Payment;
import co.edu.unbosque.marketplace.pagos.model.PaymentStatus;
import co.edu.unbosque.marketplace.pagos.repository.PaymentRepository;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final StripePaymentGateway stripeGateway;
    private final PaymentRepository repository;

    @Autowired
    public PaymentService(
            StripePaymentGateway stripeGateway,
            PaymentRepository repository) {
        this.stripeGateway = stripeGateway;
        this.repository = repository;
    }

    // Constructor compatible con las pruebas unitarias existentes.
    public PaymentService(StripePaymentGateway stripeGateway) {
        this(stripeGateway, null);
    }

    public PaymentResponseDTO cobrar(
            String claveIdempotencia,
            ChargeRequestDTO solicitud) {

        if (claveIdempotencia == null || claveIdempotencia.isBlank()) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia es obligatoria.");
        }

        try {
            UUID.fromString(claveIdempotencia);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia debe ser un UUID valido.");
        }

        String moneda = solicitud.moneda().toLowerCase(Locale.ROOT);

        if (repository != null) {
            Optional<Payment> existente =
                    repository.findByClaveIdempotencia(claveIdempotencia);

            if (existente.isPresent()) {
                Payment pago = existente.get();

                if (pago.getMonto().compareTo(solicitud.monto()) != 0
                    || !pago.getMoneda().equals(moneda)
                    || (pago.getMetodoPago() != null
                        && !pago.getMetodoPago().equals(solicitud.metodoPago()))) {
                    throw new IllegalArgumentException(
                            "La clave de idempotencia ya fue utilizada con otros datos.");
                }

                if (pago.getEstado() == PaymentStatus.PENDIENTE
        || pago.getEstado() == PaymentStatus.RESULTADO_DESCONOCIDO) {
    throw new ServicioPagoException(
            "El resultado del pago todavia no esta confirmado.",
            new IllegalStateException("Pago pendiente de conciliacion.")
    );
}

return convertirRespuesta(pago);
            }
        }
        // MKT-41: registrar intento antes de Stripe.
        Payment intentoRegistrado = null;

        if (repository != null) {
            intentoRegistrado = new Payment(
                    UUID.randomUUID(),
                    null,
                    solicitud.monto(),
                    moneda,
                    claveIdempotencia,
                    PaymentStatus.PENDIENTE
            );

            intentoRegistrado.setMetodoPago(solicitud.metodoPago());
            intentoRegistrado = repository.saveAndFlush(intentoRegistrado);
        }

        try {
            PaymentIntent intento = stripeGateway.cobrar(
                    solicitud.monto(),
                    moneda,
                    solicitud.metodoPago(),
                    claveIdempotencia
            );

            if (!"succeeded".equals(intento.getStatus())) {
                if (intentoRegistrado != null) {
                    intentoRegistrado.setStripePaymentIntentId(intento.getId());

                    if ("requires_payment_method".equals(intento.getStatus())
                            || "canceled".equals(intento.getStatus())) {
                        intentoRegistrado.setEstado(PaymentStatus.RECHAZADO);
                    } else {
                        intentoRegistrado.setEstado(PaymentStatus.RESULTADO_DESCONOCIDO);
                    }

                    repository.saveAndFlush(intentoRegistrado);
                }

                if ("requires_payment_method".equals(intento.getStatus())
                        || "canceled".equals(intento.getStatus())) {
                    throw new PagoRechazadoException(
                            "El pago fue rechazado. Estado: " + intento.getStatus());
                }

                throw new ServicioPagoException(
                        "El pago requiere conciliacion. Estado: " + intento.getStatus(),
                        new IllegalStateException("Resultado no confirmado.")
                );
            }

            Payment pago;

            if (intentoRegistrado != null) {
                pago = intentoRegistrado;
                pago.setStripePaymentIntentId(intento.getId());
                pago.setEstado(PaymentStatus.APROBADO);
                pago = repository.saveAndFlush(pago);
            } else {
                pago = new Payment(
                        UUID.randomUUID(),
                        intento.getId(),
                        solicitud.monto(),
                        moneda,
                        claveIdempotencia,
                        PaymentStatus.APROBADO
                );
                pago.setMetodoPago(solicitud.metodoPago());
            }

            return convertirRespuesta(pago);

        } catch (CardException e) {
            String mensaje = e.getMessage() == null
                    ? "La tarjeta fue rechazada."
                    : e.getMessage();

            if (intentoRegistrado != null) {
                intentoRegistrado.setEstado(PaymentStatus.RECHAZADO);
                repository.saveAndFlush(intentoRegistrado);
            }

            throw new PagoRechazadoException(mensaje);

        } catch (StripeException e) {
            if (intentoRegistrado != null) {
                intentoRegistrado.setEstado(PaymentStatus.RESULTADO_DESCONOCIDO);
                repository.saveAndFlush(intentoRegistrado);
            }

            throw new ServicioPagoException(
                    "No fue posible confirmar el resultado del pago con Stripe.",
                    e
            );
        }
    }


    // MKT-41: consultar un pago previamente registrado.
    public Optional<PaymentResponseDTO> consultarPorClave(
            String claveIdempotencia) {

        if (claveIdempotencia == null || claveIdempotencia.isBlank()) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia es obligatoria.");
        }

        try {
            UUID.fromString(claveIdempotencia);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "La clave de idempotencia debe ser un UUID valido.");
        }

        if (repository == null) {
            return Optional.empty();
        }

        return repository.findByClaveIdempotencia(claveIdempotencia)
                .map(this::convertirRespuesta);
    }
    private PaymentResponseDTO convertirRespuesta(Payment pago) {
        return new PaymentResponseDTO(
                pago.getId(),
                pago.getStripePaymentIntentId(),
                pago.getMonto(),
                pago.getEstado()
        );
    }
}