package co.edu.unbosque.marketplace.alojamientos.exception;

// La publicación no existe. Se responde 404
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
