package co.edu.unbosque.marketplace.reservas.client;

public class PagoNoDisponibleReservaException extends RuntimeException {

    public PagoNoDisponibleReservaException(
            String mensaje,
            Throwable causa) {
        super(mensaje, causa);
    }
}