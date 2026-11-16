package co.edu.unbosque.marketplace.reportes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import co.edu.unbosque.marketplace.reportes.dto.EstadisticasPublicaciones;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasReservas;
import co.edu.unbosque.marketplace.reportes.dto.EstadisticasUsuarios;
import co.edu.unbosque.marketplace.reportes.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.reportes.dto.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.reportes.exception.AutenticacionException;
import co.edu.unbosque.marketplace.reportes.exception.PermisoException;

@Component
public class ServiciosExternos {

    private static final Logger LOG = LoggerFactory.getLogger(ServiciosExternos.class);

    private final RestClient restClient;
    private final String urlIdentidad;
    private final String urlAlojamientos;
    private final String urlReservas;
    private final String urlAuditoria;

    @Autowired
    public ServiciosExternos(@Value("${servicios.identidad.url}") String urlIdentidad,
            @Value("${servicios.alojamientos.url}") String urlAlojamientos,
            @Value("${servicios.reservas.url}") String urlReservas,
            @Value("${servicios.auditoria.url}") String urlAuditoria) {
        this(RestClient.builder(), urlIdentidad, urlAlojamientos, urlReservas, urlAuditoria);
    }

    ServiciosExternos(RestClient.Builder constructor, String urlIdentidad, String urlAlojamientos, String urlReservas,
            String urlAuditoria) {
        this.restClient = constructor.build();
        this.urlIdentidad = urlIdentidad;
        this.urlAlojamientos = urlAlojamientos;
        this.urlReservas = urlReservas;
        this.urlAuditoria = urlAuditoria;
    }

    // CA-110
    public void verificarAdministrador(String autorizacion) {
        verificarRolAdministrador(autorizacion, "Solo el administrador puede ver el dashboard");
    }

    // HU-41 (CA-113): devuelve el id del administrador, que es quien exporta y queda registrado
    public Long verificarAdministradorParaExportar(String autorizacion) {
        return verificarRolAdministrador(autorizacion, "Solo el administrador puede exportar los reportes");
    }

    private Long verificarRolAdministrador(String autorizacion, String mensajeSinPermiso) {
        if (autorizacion == null || autorizacion.isBlank()) {
            throw new AutenticacionException("Debe iniciar sesión");
        }
        try {
            UsuarioIdentidadDTO usuario = restClient.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=ADMINISTRADOR")
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidadDTO.class);

            if (usuario == null || usuario.getId() == null) {
                throw new AutenticacionException("Debe iniciar sesión");
            }
            return usuario.getId();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException("Debe iniciar sesión");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException(mensajeSinPermiso);
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

    // HU-41 (CA-113): deja constancia de quién exportó, qué reporte exportó y cuándo.
    // Si Auditoría no responde, la exportación igual se entrega y el fallo queda anotado en el log.
    public boolean registrarAuditoria(EventoAuditoriaDTO evento) {
        try {
            restClient.post()
                    .uri(urlAuditoria + "/internal/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            LOG.warn("No se pudo registrar en Auditoría la acción {} sobre {}: {}",
                    evento.accion(), evento.entidad(), e.getMessage());
            return false;
        }
    }
}
