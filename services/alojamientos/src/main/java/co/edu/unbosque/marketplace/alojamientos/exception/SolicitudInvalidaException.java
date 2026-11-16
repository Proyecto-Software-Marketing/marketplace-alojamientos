package co.edu.unbosque.marketplace.alojamientos.exception;

// Datos de la publicación que no cumplen las reglas (CA-021). Se responde 400
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
