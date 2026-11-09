package co.edu.unbosque.marketplace.cotizacion.exception;

// Error cuando no hay sesion iniciada o el token no sirve
public class AutenticacionException extends RuntimeException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}