package co.edu.unbosque.marketplace.reportes.exception;

//  no es administrador 
public class PermisoException extends RuntimeException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }
}