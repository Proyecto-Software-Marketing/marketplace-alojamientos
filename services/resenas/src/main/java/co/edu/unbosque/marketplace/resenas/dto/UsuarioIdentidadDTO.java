package co.edu.unbosque.marketplace.resenas.dto;

// Lo que responde Identidad al verificar el token (solo se usan estos campos)
public record UsuarioIdentidadDTO(Long id, String rol) {
}
