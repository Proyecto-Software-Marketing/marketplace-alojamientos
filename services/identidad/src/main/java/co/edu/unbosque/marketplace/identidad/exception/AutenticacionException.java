package co.edu.unbosque.marketplace.identidad.exception;
// Error cuando no se puede iniciar sesión
public class AutenticacionException extends RuntimeException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}