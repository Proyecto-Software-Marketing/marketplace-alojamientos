package co.edu.unbosque.marketplace.busqueda.client;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionesDisponibles;
import org.springframework.http.MediaType;


@Component
public class ServiciosExternos {

    private final RestClient restClient = RestClient.create();

    @Value("${servicios.alojamientos.url}")
    private String urlAlojamientos;

    @Value("${servicios.disponibilidad.url}")
    private String urlDisponibilidad;

    // Le pide a Alojamientos las publicaciones activas de una ciudad
    public List<PublicacionResumen> buscarPublicacionesActivas(String ciudad) {
        // si no hay direccion configurada, no hay publicaciones que mostrar
        if (urlAlojamientos.isBlank()) {
            return List.of();
        }
        PublicacionResumen[] respuesta = restClient.get()
                .uri(urlAlojamientos + "/interno/publicaciones/activas?ciudad={ciudad}", ciudad)
                .retrieve()
                .body(PublicacionResumen[].class);
        if (respuesta == null) {
            return List.of();
        }
        return Arrays.asList(respuesta);
    }

    // Le pregunta a Disponibilidad si la publicacion esta libre en esas fechas
    public boolean estaLibre(Long idPublicacion, LocalDate llegada, LocalDate salida) {

        if (urlDisponibilidad.isBlank()) {
            return true;
        }
   
        Map<String, Object> consulta = Map.of(
                "publicacionIds", List.of(idPublicacion),
                "fechaLlegada", llegada.toString(),
                "fechaSalida", salida.toString());

        PublicacionesDisponibles respuesta = restClient.post()
                .uri(urlDisponibilidad + "/internal/availability/check")
                .contentType(MediaType.APPLICATION_JSON)
                .body(consulta)
                .retrieve()
                .body(PublicacionesDisponibles.class);

        return respuesta != null && respuesta.getDisponibles() != null
                && respuesta.getDisponibles().contains(idPublicacion);
    }
}