package co.edu.unbosque.marketplace.alojamientos.exception;

// El usuario tiene sesión pero su rol no puede hacer la operación (CA-022). Se responde 403
public class PermisoException extends RuntimeException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }
}
