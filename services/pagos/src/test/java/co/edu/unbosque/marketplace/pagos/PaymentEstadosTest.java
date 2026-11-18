package co.edu.unbosque.marketplace.pagos;

import co.edu.unbosque.marketplace.pagos.dto.ChargeRequestDTO;
import co.edu.unbosque.marketplace.pagos.exception.PagoRechazadoException;
import co.edu.unbosque.marketplace.pagos.exception.ServicioPagoException;
import co.edu.unbosque.marketplace.pagos.model.Payment;
import co.edu.unbosque.marketplace.pagos.model.PaymentStatus;
import co.edu.unbosque.marketplace.pagos.repository.PaymentRepository;
import co.edu.unbosque.marketplace.pagos.service.PaymentService;
import co.edu.unbosque.marketplace.pagos.service.StripePaymentGateway;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentEstadosTest {

    private StripePaymentGateway gateway;
    private PaymentRepository repository;
    private PaymentService servicio;

    private final String clave = UUID.randomUUID().toString();

    private ChargeRequestDTO solicitud() {
        return new ChargeRequestDTO(
                new BigDecimal("50000.00"),
                "cop",
                "pm_card_visa"
        );
    }

    @BeforeEach
    void preparar() {
        gateway = mock(StripePaymentGateway.class);
        repository = mock(PaymentRepository.class);
        servicio = new PaymentService(gateway, repository);
    }

    private Payment pagoConEstado(PaymentStatus estado) {
        Payment pago = new Payment(
                UUID.randomUUID(),
                null,
                new BigDecimal("50000.00"),
                "cop",
                clave,
                estado
        );
        pago.setMetodoPago("pm_card_visa");
        return pago;
    }

    @Test
    void pagoPendienteNoSeVuelveACobrar() {
        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.of(pagoConEstado(PaymentStatus.PENDIENTE)));

        assertThrows(
                ServicioPagoException.class,
                () -> servicio.cobrar(clave, solicitud())
        );

        verifyNoInteractions(gateway);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void resultadoDesconocidoNoSeVuelveACobrar() {
        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.of(pagoConEstado(PaymentStatus.RESULTADO_DESCONOCIDO)));

        assertThrows(
                ServicioPagoException.class,
                () -> servicio.cobrar(clave, solicitud())
        );

        verifyNoInteractions(gateway);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void stripeRechazaYSeGuardaEstadoRechazado() throws Exception {
        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.empty());

        when(repository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PaymentIntent intento = new PaymentIntent();
        intento.setId("pi_test_rechazado");
        intento.setStatus("requires_payment_method");

        when(gateway.cobrar(any(), anyString(), anyString(), eq(clave)))
                .thenReturn(intento);

        assertThrows(
                PagoRechazadoException.class,
                () -> servicio.cobrar(clave, solicitud())
        );

        verify(repository, times(2)).saveAndFlush(
                argThat(pago -> pago.getEstado() == PaymentStatus.RECHAZADO)
        );

        verify(gateway, times(1))
                .cobrar(any(), anyString(), anyString(), eq(clave));
    }
}