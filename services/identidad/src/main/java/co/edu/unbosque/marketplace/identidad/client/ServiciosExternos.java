package co.edu.unbosque.marketplace.identidad.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

//Preguntas que este servicio le hace a los otros servicios
@Component
public class ServiciosExternos {

    private final RestClient cliente = RestClient.create();

    // Direcciones de los otros servicios, se ponen en application.properties
    @Value("${servicios.reservas.url}")
    private String urlReservas;

    @Value("${servicios.alojamientos.url}")
    private String urlAlojamientos;

    @Value("${servicios.notificaciones.url}")
    private String urlNotificaciones;

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

    // HU-04 CA-010 le pide a Notificaciones que muestre el token en una notificación interna.
    // El id de la solicitud va como entidadId para que cada solicitud sea una notificación nueva
    public void notificarRecuperacion(Long idUsuario, Long idRecuperacion, String mensaje) {
        // Si todavía no está el servicio de Notificaciones no se hace nada
        if (urlNotificaciones.isBlank()) {
            return;
        }
        cliente.post()
                .uri(urlNotificaciones + "/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "usuarioId", idUsuario,
                        "tipoEvento", "RECUPERACION_CONTRASENA",
                        "entidadTipo", "USUARIO",
                        "entidadId", idRecuperacion,
                        "mensaje", mensaje))
                .retrieve()
                .toBodilessEntity();
    }
}