package co.edu.unbosque.marketplace.busqueda.client;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;


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
        // si Disponibilidad no esta configurado, se asume que esta libre
        if (urlDisponibilidad.isBlank()) {
            return true;
        }
        Boolean libre = restClient.get()
                .uri(urlDisponibilidad + "/interno/disponibilidad/esta-libre?idPublicacion={id}&llegada={llegada}&salida={salida}",
                        idPublicacion, llegada, salida)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(libre);
    }
}