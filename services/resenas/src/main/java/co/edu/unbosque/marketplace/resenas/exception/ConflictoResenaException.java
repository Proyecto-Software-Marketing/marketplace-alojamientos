package co.edu.unbosque.marketplace.resenas.exception;

// La reserva no está COMPLETADA o ya tiene reseña (CA-089 y CA-090). Se responde 409
public class ConflictoResenaException extends RuntimeException {

    public ConflictoResenaException(String mensaje) {
        super(mensaje);
    }
}
