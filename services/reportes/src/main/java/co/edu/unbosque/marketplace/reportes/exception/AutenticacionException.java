package co.edu.unbosque.marketplace.reportes.exception;

// No inicio sesion o la sesion no sirve 
public class AutenticacionException extends RuntimeException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}