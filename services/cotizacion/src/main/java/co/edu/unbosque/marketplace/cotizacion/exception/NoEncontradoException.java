package co.edu.unbosque.marketplace.cotizacion.exception;

// HU-22: La publicación o la cotización no existe. Se responde 404
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
