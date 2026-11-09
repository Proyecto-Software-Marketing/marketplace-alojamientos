package co.edu.unbosque.marketplace.identidad.exception;

// Error cuando no se puede desactivar la cuenta
public class CuentaException extends RuntimeException {

    public CuentaException(String mensaje) {
        super(mensaje);
    }
}