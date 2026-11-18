package co.edu.unbosque.marketplace.reservas.client;

public class PagoRechazadoReservaException extends RuntimeException {

    public PagoRechazadoReservaException(String mensaje) {
        super(mensaje);
    }
}