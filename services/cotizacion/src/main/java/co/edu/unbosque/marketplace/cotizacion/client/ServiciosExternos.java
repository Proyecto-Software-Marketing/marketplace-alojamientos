package co.edu.unbosque.marketplace.cotizacion.client;

import co.edu.unbosque.marketplace.cotizacion.dto.UsuarioIdentidad;
import co.edu.unbosque.marketplace.cotizacion.exception.AutenticacionException;
import co.edu.unbosque.marketplace.cotizacion.exception.PermisoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Map;

// Lo que necesita corizacion de otros servicios
@Component
public class ServiciosExternos {

    private static final Logger log = LoggerFactory.getLogger(ServiciosExternos.class);

    private final RestClient cliente = RestClient.create();

    @Value("${servicios.identidad.url}")
    private String urlIdentidad;

    @Value("${servicios.auditoria.url}")
    private String urlAuditoria;

    // Le pregunta a Identidad si el token es de un administrador y devuelve su id
    public Long verificarAdministrador(String autorizacion) {
        try {
            UsuarioIdentidad usuario = cliente.get()
                    .uri(urlIdentidad + "/autenticacion/autorizar?roles=ADMINISTRADOR")
                    .header("Authorization", autorizacion)
                    .retrieve()
                    .body(UsuarioIdentidad.class);

            if (usuario == null) {
                throw new AutenticacionException("Debe iniciar sesión");
            }
            return usuario.getId();

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AutenticacionException("Debe iniciar sesión");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PermisoException("Solo el administrador puede cambiar la tarifa de servicio");
        }
    }

    // CA-107 le avisa a Auditoría que se cambio la tarifa
    public void registrarCambioTarifa(Long idAdministrador, BigDecimal anterior, BigDecimal nuevo) {
        // Si todavía no está el servicio de Auditoría no se hace nada
        if (urlAuditoria.isBlank()) {
            return;
        }
        try {
            cliente.post()
                    .uri(urlAuditoria + "/interno/auditoria/eventos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "accion", "CAMBIO_TARIFA_SERVICIO",
                            "idUsuario", idAdministrador,
                            "detalle", "La tarifa de servicio pasó de " + anterior + "% a " + nuevo + "%"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo registrar el cambio de tarifa en Auditoría", e);
        }
    }
}