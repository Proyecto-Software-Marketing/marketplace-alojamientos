package co.edu.unbosque.marketplace.notificaciones.exception;

// No hay sesión o el token no es válido. Se responde 401
public class AutenticacionException extends RuntimeException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
