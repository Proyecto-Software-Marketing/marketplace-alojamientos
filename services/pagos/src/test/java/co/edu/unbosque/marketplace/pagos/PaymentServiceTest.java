package co.edu.unbosque.marketplace.pagos;

import co.edu.unbosque.marketplace.pagos.dto.ChargeRequestDTO;
import co.edu.unbosque.marketplace.pagos.dto.PaymentResponseDTO;
import co.edu.unbosque.marketplace.pagos.exception.PagoRechazadoException;
import co.edu.unbosque.marketplace.pagos.model.PaymentStatus;
import co.edu.unbosque.marketplace.pagos.service.PaymentService;
import co.edu.unbosque.marketplace.pagos.service.StripePaymentGateway;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private StripePaymentGateway gateway;
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
        gateway = Mockito.mock(StripePaymentGateway.class);
        servicio = new PaymentService(gateway);
    }

    @Test
    void pagoAprobado() throws Exception {
        PaymentIntent intento = new PaymentIntent();
        intento.setId("pi_test_aprobado");
        intento.setStatus("succeeded");

        when(gateway.cobrar(any(), anyString(), anyString(), anyString()))
                .thenReturn(intento);

        PaymentResponseDTO respuesta =
                servicio.cobrar(clave, solicitud());

        assertEquals(PaymentStatus.APROBADO, respuesta.estado());
        assertEquals("pi_test_aprobado", respuesta.stripePaymentIntentId());
        assertNotNull(respuesta.id());

        verify(gateway, times(1))
                .cobrar(any(), anyString(), anyString(), eq(clave));
    }

    @Test
    void pagoRechazado() throws Exception {
        when(gateway.cobrar(any(), anyString(), anyString(), anyString()))
                .thenThrow(new CardException(
                        "Tarjeta rechazada",
                        "req_test",
                        "card_declined",
                        "declined",
                        "card_declined",
                        null,
                        402,
                        null
                ));

        PagoRechazadoException error = assertThrows(
                PagoRechazadoException.class,
                () -> servicio.cobrar(clave, solicitud())
        );

        assertTrue(error.getMessage().contains("Tarjeta rechazada"));
    }

    @Test
    void claveIdempotenciaInvalida() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.cobrar("clave-invalida", solicitud())
        );

        verifyNoInteractions(gateway);
    }
}