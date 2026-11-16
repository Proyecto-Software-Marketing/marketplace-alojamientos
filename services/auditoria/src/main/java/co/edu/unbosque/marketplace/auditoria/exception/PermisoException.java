package co.edu.unbosque.marketplace.auditoria.exception;

// El usuario no tiene el rol necesario
public class PermisoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PermisoException(String mensaje) {
		super(mensaje);
	}

}
