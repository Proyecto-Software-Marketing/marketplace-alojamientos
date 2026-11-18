package co.edu.unbosque.marketplace.pagos;

import co.edu.unbosque.marketplace.pagos.model.Payment;
import co.edu.unbosque.marketplace.pagos.model.PaymentStatus;
import co.edu.unbosque.marketplace.pagos.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentPersistenciaTest {

    @Autowired
    private PaymentRepository repository;

    @Test
    void guardarYRecuperarPagoPendiente() {

        String clave = UUID.randomUUID().toString();

        Payment pago = new Payment(
                UUID.randomUUID(),
                null,
                new BigDecimal("50000.00"),
                "cop",
                clave,
                PaymentStatus.PENDIENTE
        );

        pago.setMetodoPago("pm_card_visa");

        repository.saveAndFlush(pago);

        Payment recuperado = repository
                .findByClaveIdempotencia(clave)
                .orElseThrow();

        assertEquals(PaymentStatus.PENDIENTE,
                recuperado.getEstado());

        assertEquals("cop", recuperado.getMoneda());

        assertEquals("pm_card_visa",
                recuperado.getMetodoPago());

        assertNull(recuperado.getStripePaymentIntentId());
    }
}