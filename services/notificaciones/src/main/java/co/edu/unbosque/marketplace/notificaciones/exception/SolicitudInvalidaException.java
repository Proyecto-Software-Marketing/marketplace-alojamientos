package co.edu.unbosque.marketplace.notificaciones.exception;

// El evento recibido no es válido (tipo desconocido o entidad que no corresponde). Se responde 400
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
