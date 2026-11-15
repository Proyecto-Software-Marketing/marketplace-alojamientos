package co.edu.unbosque.marketplace.reservas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Respuesta de GET /autenticacion/autorizar de Identidad
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioIdentidadDTO(Long id, String rol) {
}
