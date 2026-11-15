package co.edu.unbosque.marketplace.resenas.exception;

// El usuario tiene sesión pero su rol no puede hacer la operación. Se responde 403
public class PermisoException extends RuntimeException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }
}
