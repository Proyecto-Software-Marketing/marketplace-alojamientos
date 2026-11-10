package co.edu.unbosque.marketplace.cotizacion.exception;

// Error cuando el usuario no es administrador
public class PermisoException extends RuntimeException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }
}