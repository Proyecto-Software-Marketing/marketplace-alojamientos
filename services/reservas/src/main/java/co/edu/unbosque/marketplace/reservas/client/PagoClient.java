package co.edu.unbosque.marketplace.reservas.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class PagoClient {

    private final RestClient cliente;
    private final String urlPagos;

    public PagoClient(
            @Value("${servicios.pagos.url}") String urlPagos) {
        this.cliente = RestClient.create();
        this.urlPagos = urlPagos;
    }

    public PagoRespuestaDTO cobrar(
            Long huespedId,
            String idempotencyKey,
            BigDecimal monto,
            String metodoPago) {

        String clavePago = UUID.nameUUIDFromBytes(
                ("reserva:" + huespedId + ":" + idempotencyKey)
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();

        PagoSolicitudDTO solicitud = new PagoSolicitudDTO(
                monto,
                "cop",
                metodoPago
        );

        try {
            PagoRespuestaDTO respuesta = cliente.post()
                    .uri(urlPagos + "/internal/payments")
                    .header("Idempotency-Key", clavePago)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .body(PagoRespuestaDTO.class);

            if (respuesta == null) {
                throw new IllegalStateException(
                        "El servicio de pagos no devolvio una respuesta."
                );
            }

            return respuesta;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() != 402) {
                throw new PagoNoDisponibleReservaException(
                        "El servicio de pagos devolvio un error HTTP.",
                        e
                );
            }
            throw new PagoRechazadoReservaException(
                    "El pago fue rechazado por Stripe."
            );

        } catch (RestClientException e) {
            throw new PagoNoDisponibleReservaException(
                    "No fue posible confirmar el resultado del pago.",
                    e
            );
        }
    }
}