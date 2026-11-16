package co.edu.unbosque.marketplace.alojamientos.exception;

// La publicación está en un estado que no permite la operación (HU-09, decisión 4). Se responde 409
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
