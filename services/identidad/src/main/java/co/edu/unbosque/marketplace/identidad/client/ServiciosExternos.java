package co.edu.unbosque.marketplace.identidad.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

//Preguntas que este servicio le hace a los otros servicios
@Component
public class ServiciosExternos {

    private final RestClient cliente = RestClient.create();

    // Direcciones de los otros servicios, se ponen en application.properties
    @Value("${servicios.reservas.url}")
    private String urlReservas;

    @Value("${servicios.alojamientos.url}")
    private String urlAlojamientos;

    // CA-017 le pregunta a Reservas si el usuario tiene reservas futuras confirmadas
    public boolean tieneReservasFuturas(Long idUsuario) {
        // Si todavía no está el servicio de Reservas no se pregunta
        if (urlReservas.isBlank()) {
            return false;
        }
        Boolean respuesta = cliente.get()
                .uri(urlReservas + "/interno/reservas/futuras?idUsuario=" + idUsuario)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(respuesta);
    }

    // CA-018 le pide a Alojamientos que pause las publicaciones del anfitrión
    public void pausarPublicaciones(Long idAnfitrion) {
        // Si todavía no está el servicio de Alojamientos no se hace nada
        if (urlAlojamientos.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlAlojamientos + "/interno/publicaciones/pausar?idAnfitrion=" + idAnfitrion)
                .retrieve()
                .toBodilessEntity();
    }
}