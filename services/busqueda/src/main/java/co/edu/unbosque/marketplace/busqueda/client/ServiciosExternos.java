package co.edu.unbosque.marketplace.busqueda.client;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.busqueda.dto.PublicacionDetalle;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionesDisponibles;

// Aqui estan las llamadas a los otros servicios
@Component
public class ServiciosExternos {

    private final RestClient restClient;
    private final String urlAlojamientos;
    private final String urlDisponibilidad;

    @Autowired
    public ServiciosExternos(@Value("${servicios.alojamientos.url}") String urlAlojamientos,
            @Value("${servicios.disponibilidad.url}") String urlDisponibilidad) {
        this(RestClient.builder(), urlAlojamientos, urlDisponibilidad);
    }

    ServiciosExternos(RestClient.Builder constructor, String urlAlojamientos, String urlDisponibilidad) {
        this.restClient = constructor.build();
        this.urlAlojamientos = urlAlojamientos;
        this.urlDisponibilidad = urlDisponibilidad;
    }

    // Le pide a Alojamientos las publicaciones activas de una ciudad
    public List<PublicacionResumen> buscarPublicacionesActivas(String ciudad) {
       
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

    // Le pide a Alojamientos el detalle completo de una publicacion 
    public PublicacionDetalle obtenerDetalle(Long idPublicacion) {
        
        if (urlAlojamientos.isBlank()) {
            return null;
        }
        try {
            return restClient.get()
                    .uri(urlAlojamientos + "/interno/publicaciones/{id}", idPublicacion)
                    .retrieve()
                    .body(PublicacionDetalle.class);
        } catch (HttpClientErrorException.NotFound e) {
            
            return null;
        }
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