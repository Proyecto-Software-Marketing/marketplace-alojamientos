package co.edu.unbosque.marketplace.notificaciones.exception;

// Identidad no respondió. Se responde 503
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
