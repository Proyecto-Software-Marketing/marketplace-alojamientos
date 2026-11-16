package co.edu.unbosque.marketplace.identidad.exception;

// HU-04 CA-012 el token de recuperación está vencido, ya se usó o no existe
public class TokenRecuperacionException extends RuntimeException {

    public TokenRecuperacionException(String mensaje) {
        super(mensaje);
    }
}
