package co.edu.unbosque.marketplace.disponibilidad.exception;

// El periodo o la publicación no existe. Se responde 404
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}