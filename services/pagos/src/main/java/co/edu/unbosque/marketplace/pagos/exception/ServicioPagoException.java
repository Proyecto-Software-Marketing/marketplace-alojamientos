package co.edu.unbosque.marketplace.pagos.exception;

public class ServicioPagoException extends RuntimeException {

    public ServicioPagoException(String mensaje) {
        super(mensaje);
    }

    public ServicioPagoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}