package co.edu.unbosque.marketplace.pagos.exception;

public class PagoRechazadoException extends RuntimeException {

    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}