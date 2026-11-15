package co.edu.unbosque.marketplace.identidad.exception;

// Otro servicio que se necesita no respondió
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
