package co.edu.unbosque.marketplace.resenas.exception;

// Identidad o Reservas no respondieron como se esperaba. Se responde 503
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
