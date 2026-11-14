package co.edu.unbosque.marketplace.disponibilidad.exception;

// Las fechas del periodo no cumplen las reglas (CA-046 y decisión 3 de la HU-16). Se responde 400
public class FechasInvalidasException extends RuntimeException {

    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}