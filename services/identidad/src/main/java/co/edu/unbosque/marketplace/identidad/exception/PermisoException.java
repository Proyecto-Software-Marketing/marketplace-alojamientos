package co.edu.unbosque.marketplace.identidad.exception;

 //Error cuando el usuario ya inicio sesión pero su rol no le permite hacer alguna acción

public class PermisoException extends RuntimeException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }
}