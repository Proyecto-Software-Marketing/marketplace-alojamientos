package co.edu.unbosque.marketplace.auditoria.exception;

// No hay sesion o la sesion no sirve
public class AutenticacionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AutenticacionException(String mensaje) {
		super(mensaje);
	}

}
