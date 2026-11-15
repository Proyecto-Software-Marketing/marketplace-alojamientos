package co.edu.unbosque.marketplace.reservas.client;

public record UsuarioIdentidadDTO(
        Long id,
        String correo,
        String nombre,
        String rol) {
}
