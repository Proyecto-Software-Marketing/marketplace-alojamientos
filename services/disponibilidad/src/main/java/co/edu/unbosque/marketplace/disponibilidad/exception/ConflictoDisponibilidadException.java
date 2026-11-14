package co.edu.unbosque.marketplace.disponibilidad.exception;

// La operación choca con otro periodo o con reservas confirmadas (CA-047 y decisión 2 de la HU-16). Se responde 409
public class ConflictoDisponibilidadException extends RuntimeException {

    public ConflictoDisponibilidadException(String mensaje) {
        super(mensaje);
    }
}