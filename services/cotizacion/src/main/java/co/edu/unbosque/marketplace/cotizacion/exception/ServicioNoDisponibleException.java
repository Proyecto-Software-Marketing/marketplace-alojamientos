package co.edu.unbosque.marketplace.cotizacion.exception;

// HU-22: Identidad o Alojamientos no respondieron. Se responde 503
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
