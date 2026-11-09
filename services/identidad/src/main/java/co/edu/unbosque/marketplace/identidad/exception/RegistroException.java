package co.edu.unbosque.marketplace.identidad.exception;

// Error al crear registro 
public class RegistroException extends RuntimeException {

    public RegistroException(String mensaje) {
        super(mensaje);
    }
}