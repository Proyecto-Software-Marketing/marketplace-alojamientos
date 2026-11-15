package co.edu.unbosque.marketplace.busqueda.exception;

// Cuando la publicacion no existe o no esta activa
public class PublicacionNoEncontradaException extends RuntimeException {

    public PublicacionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
