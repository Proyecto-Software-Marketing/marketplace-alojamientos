package co.edu.unbosque.marketplace.reservas.exception;

// CA-065: el periodo ya no está disponible al confirmar la reserva
public class ConflictoReservaException extends RuntimeException {

    public ConflictoReservaException(String mensaje) {
        super(mensaje);
    }
}
