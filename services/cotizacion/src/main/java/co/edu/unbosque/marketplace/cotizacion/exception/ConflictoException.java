package co.edu.unbosque.marketplace.cotizacion.exception;

// HU-22: La publicación no está ACTIVA y no se puede cotizar (decisión 5). Se responde 409
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
