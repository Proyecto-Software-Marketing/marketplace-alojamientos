package co.edu.unbosque.marketplace.cotizacion.exception;

// HU-22: Fechas o servicios adicionales que no cumplen las reglas (decisiones 4 y 7). Se responde 400
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
