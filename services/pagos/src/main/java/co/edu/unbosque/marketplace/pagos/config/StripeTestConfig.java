package co.edu.unbosque.marketplace.pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeTestConfig {

    private final String secretKey;
    private final String currency;

    public StripeTestConfig(
            @Value("${stripe.secret-key:}") String secretKey,
            @Value("${stripe.currency:cop}") String currency) {

        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.currency = currency == null ? "cop" : currency.trim().toLowerCase();
    }

    public String getSecretKey() {
        if (!secretKey.startsWith("sk_test_") || secretKey.length() <= 8) {
            throw new IllegalStateException(
                    "Stripe requiere una clave secreta valida de modo prueba (sk_test_).");
        }
        return secretKey;
    }

    public String getCurrency() {
        return currency;
    }
}