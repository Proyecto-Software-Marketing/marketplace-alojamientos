package co.edu.unbosque.marketplace.disponibilidad.exception;

// Hay sesión, pero el usuario no es anfitrión o no es el dueño de la publicación. Se responde 403
public class PermisoException extends RuntimeException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }
}