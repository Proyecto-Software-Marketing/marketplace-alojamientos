package co.edu.unbosque.marketplace.resenas.exception;

// La reserva no existe o no es del usuario (no se revela cuál de los dos). Se responde 404
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
