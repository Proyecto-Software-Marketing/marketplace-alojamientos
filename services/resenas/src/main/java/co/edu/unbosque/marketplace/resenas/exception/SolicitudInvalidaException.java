package co.edu.unbosque.marketplace.resenas.exception;

// Datos de la reseña que no cumplen las reglas (CA-091). Se responde 400
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
