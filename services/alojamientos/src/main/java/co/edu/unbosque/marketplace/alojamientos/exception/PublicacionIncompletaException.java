
package co.edu.unbosque.marketplace.alojamientos.exception;

// HU-12: publicacion incompleta para enviar a revision.
public class PublicacionIncompletaException extends RuntimeException {

    public PublicacionIncompletaException(String mensaje) {
        super(mensaje);
    }
}
