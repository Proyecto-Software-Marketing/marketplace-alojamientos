package co.edu.unbosque.marketplace.pagos.service;

import co.edu.unbosque.marketplace.pagos.config.StripeTestConfig;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class StripePaymentGateway {

    private final StripeTestConfig config;

    public StripePaymentGateway(StripeTestConfig config) {
        this.config = config;
    }

    public PaymentIntent cobrar(
            BigDecimal monto,
            String moneda,
            String metodoPago,
            String claveIdempotencia) throws StripeException {

        String monedaNormalizada = moneda.toLowerCase(Locale.ROOT);

        if (!monedaNormalizada.equals(config.getCurrency())) {
            throw new IllegalArgumentException(
                    "La moneda solicitada no coincide con la configurada.");
        }

        // Para USD, Stripe recibe centavos.
        long montoEnCentavos = monto
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();

        if (montoEnCentavos <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor a cero.");
        }

        StripeClient cliente = new StripeClient(config.getSecretKey());

        PaymentIntentCreateParams parametros =
                PaymentIntentCreateParams.builder()
                        .setAmount(montoEnCentavos)
                        .setCurrency(monedaNormalizada)
                        .setPaymentMethod(metodoPago)
                        .setConfirm(true)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .setAllowRedirects(
                                                PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                        .build())
                        .build();

        RequestOptions opciones = RequestOptions.builder()
                .setIdempotencyKey(claveIdempotencia)
                .build();

        return cliente.paymentIntents()
                .create(parametros, opciones);
    }
}