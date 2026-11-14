package co.edu.unbosque.marketplace.disponibilidad.exception;

// Identidad o Alojamientos no respondieron, así que no se puede verificar el permiso. Se responde 503
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}