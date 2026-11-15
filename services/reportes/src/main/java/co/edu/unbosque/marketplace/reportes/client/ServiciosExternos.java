package co.edu.unbosque.marketplace.reportes.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.reportes.dto.EstadisticasPublicaciones;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasReservas;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasUsuarios;
import co.edu.unbosque.marketplace.reportes.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;


@Component
public class ServiciosExternos {

    private final RestClient restClient;
    private final String urlIdentidad;
    private final String urlAlojamientos;
    private final String urlReservas;

    @Autowired
    public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.alojamientos.url}") String urlAlojamientos,
            @Value("${servicios.reservas.url}") String urlReservas) {
        this(RestClient.builder(), urlIdentidad, urlAlojamientos, urlReservas);
    }


    ServiciosExternos(RestClient.Builder constructor, String urlIdentidad, String urlAlojamientos, String urlReservas) {
        this.restClient = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlAlojamientos = urlAlojamientos;
        this.urlReservas = urlReservas;
    }

    // CA-110
    public void verificarAdministrador(String autorizacion) {
        if (autorizacion == null || autorizacion.isBlank()) {
            throw new AutenticacionException("Debe iniciar sesión");
        }
        try {
            restClient.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=ADMINISTRADOR")
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException("Debe iniciar sesión");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException("Solo el administrador puede ver el dashboard");
        }
    }


    public EstadisticasUsuarios estadisticasUsuarios() {
        EstadisticasUsuarios respuesta = restClient.get()
                .uri(urlIdentidad + "/interno/usuarios/estadisticas")
                .retrieve()
                .body(EstadisticasUsuarios.class);
        return respuesta == null ? new EstadisticasUsuarios() : respuesta;
    }

   
    public EstadisticasPublicaciones estadisticasPublicaciones() {
        // si Alojamientos no esta configurado, queda en cero
        if (urlAlojamientos.isBlank()) {
            return new EstadisticasPublicaciones();
        }
        EstadisticasPublicaciones respuesta = restClient.get()
                .uri(urlAlojamientos + "/interno/publicaciones/estadisticas")
                .retrieve()
                .body(EstadisticasPublicaciones.class);
        return respuesta == null ? new EstadisticasPublicaciones() : respuesta;
    }

   
    public EstadisticasReservas estadisticasReservas() {
        // si Reservas no esta configurado, queda en cero
        if (urlReservas.isBlank()) {
            return new EstadisticasReservas();
        }
        EstadisticasReservas respuesta = restClient.get()
                .uri(urlReservas + "/interno/reservas/estadisticas")
                .retrieve()
                .body(EstadisticasReservas.class);
        return respuesta == null ? new EstadisticasReservas() : respuesta;
    }
}