package co.edu.unbosque.marketplace.alojamientos.exception;

// Identidad no respondió, así que no se puede verificar el permiso. Se responde 503
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
