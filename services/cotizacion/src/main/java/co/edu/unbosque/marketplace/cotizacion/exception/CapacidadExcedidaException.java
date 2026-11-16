package co.edu.unbosque.marketplace.cotizacion.exception;

// HU-22: CA-063: más huéspedes que la capacidad. Se responde 422
public class CapacidadExcedidaException extends RuntimeException {

    public CapacidadExcedidaException(String mensaje) {
        super(mensaje);
    }
}
