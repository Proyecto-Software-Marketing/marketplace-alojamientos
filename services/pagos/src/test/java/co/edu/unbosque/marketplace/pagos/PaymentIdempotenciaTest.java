package co.edu.unbosque.marketplace.pagos;

import co.edu.unbosque.marketplace.pagos.dto.ChargeRequestDTO;
import co.edu.unbosque.marketplace.pagos.dto.PaymentResponseDTO;
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

class PaymentIdempotenciaTest {

    private StripePaymentGateway gateway;
    private PaymentRepository repository;
    private PaymentService servicio;

    private final String clave = UUID.randomUUID().toString();

    private ChargeRequestDTO solicitud() {
        return new ChargeRequestDTO(
                new BigDecimal("50.00"),
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

    @Test
    void solicitudRepetidaDevuelveMismoPago() {
        UUID id = UUID.randomUUID();

        Payment pago = new Payment(
                id,
                "pi_test_existente",
                new BigDecimal("50.00"),
                "cop",
                clave,
                PaymentStatus.APROBADO
        );

        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.of(pago));

        PaymentResponseDTO respuesta =
                servicio.cobrar(clave, solicitud());

        assertEquals(id, respuesta.id());
        assertEquals("pi_test_existente", respuesta.stripePaymentIntentId());
        assertEquals(PaymentStatus.APROBADO, respuesta.estado());

        verifyNoInteractions(gateway);
        verify(repository, never()).save(any());
    }

    @Test
    void pagoNuevoSeGuardaUnaVez() throws Exception {
        PaymentIntent intento = new PaymentIntent();
        intento.setId("pi_test_nuevo");
        intento.setStatus("succeeded");

        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.empty());

        when(gateway.cobrar(any(), anyString(), anyString(), eq(clave)))
                .thenReturn(intento);

        when(repository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PaymentResponseDTO respuesta =
                servicio.cobrar(clave, solicitud());

        assertEquals(PaymentStatus.APROBADO, respuesta.estado());
        assertEquals("pi_test_nuevo", respuesta.stripePaymentIntentId());

        verify(gateway, times(1))
                .cobrar(any(), anyString(), anyString(), eq(clave));

        verify(repository, times(2)).saveAndFlush(any(Payment.class));
    }

    @Test
    void mismaClaveConOtroMontoEsRechazada() {
        Payment pago = new Payment(
                UUID.randomUUID(),
                "pi_test_existente",
                new BigDecimal("50.00"),
                "cop",
                clave,
                PaymentStatus.APROBADO
        );

        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.of(pago));

        ChargeRequestDTO otraSolicitud = new ChargeRequestDTO(
                new BigDecimal("100.00"),
                "cop",
                "pm_card_visa"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.cobrar(clave, otraSolicitud)
        );

        verifyNoInteractions(gateway);
    }

    @Test
    void consultarPagoExistentePorClave() {
        UUID id = UUID.randomUUID();

        Payment pago = new Payment(
                id,
                "pi_test_consulta",
                new BigDecimal("50000.00"),
                "cop",
                clave,
                PaymentStatus.APROBADO
        );

        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.of(pago));

        Optional<PaymentResponseDTO> resultado =
                servicio.consultarPorClave(clave);

        assertTrue(resultado.isPresent());
        assertEquals(id, resultado.get().id());
        assertEquals("pi_test_consulta",
                resultado.get().stripePaymentIntentId());

        verifyNoInteractions(gateway);
    }

    @Test
    void consultarPagoInexistenteDevuelveVacio() {
        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.empty());

        Optional<PaymentResponseDTO> resultado =
                servicio.consultarPorClave(clave);

        assertTrue(resultado.isEmpty());

        verifyNoInteractions(gateway);
    }

    @Test
    void consultarConClaveInvalidaEsRechazado() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultarPorClave("clave-invalida")
        );

        verifyNoInteractions(gateway);
        verifyNoInteractions(repository);
    }

    @Test
    void mismaClaveConOtroMetodoPagoEsRechazada() {
        Payment pago = new Payment(
                UUID.randomUUID(),
                "pi_test_existente",
                new BigDecimal("50000.00"),
                "cop",
                clave,
                PaymentStatus.APROBADO
        );

        pago.setMetodoPago("pm_card_visa");

        when(repository.findByClaveIdempotencia(clave))
                .thenReturn(Optional.of(pago));

        ChargeRequestDTO otraSolicitud = new ChargeRequestDTO(
                new BigDecimal("50000.00"),
                "cop",
                "pm_card_mastercard"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.cobrar(clave, otraSolicitud)
        );

        verifyNoInteractions(gateway);
        verify(repository, never()).save(any(Payment.class));
    }
}