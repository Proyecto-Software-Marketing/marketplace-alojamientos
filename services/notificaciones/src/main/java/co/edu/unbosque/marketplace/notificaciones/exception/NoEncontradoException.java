package co.edu.unbosque.marketplace.notificaciones.exception;

// La notificación no existe o no es del usuario. Se responde 404
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
