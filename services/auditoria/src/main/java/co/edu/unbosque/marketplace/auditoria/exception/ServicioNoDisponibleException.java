package co.edu.unbosque.marketplace.auditoria.exception;

// Otro servicio (Identidad) no responde
public class ServicioNoDisponibleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServicioNoDisponibleException(String mensaje) {
		super(mensaje);
	}

}
