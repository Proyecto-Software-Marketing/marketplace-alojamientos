package co.edu.unbosque.marketplace.auditoria.exception;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import co.edu.unbosque.marketplace.auditoria.service.AuditoriaLogger;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ControlErrores {

	private final AuditoriaLogger logger;
	private final Clock reloj;

	public ControlErrores(AuditoriaLogger logger, Clock reloj) {
		this.logger = logger;
		this.reloj = reloj;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, List<String>>> validacion(MethodArgumentNotValidException excepcion,
			HttpServletRequest solicitud) {
		List<String> mensajes = excepcion.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.filter(mensaje -> mensaje != null && !mensaje.isBlank())
				.distinct()
				.sorted()
				.toList();

		List<String> campos = excepcion.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getField)
				.distinct()
				.sorted()
				.toList();

		registrar("VALIDACION", "Campos inválidos: " + String.join(", ", campos),
				"MethodArgumentNotValidException", solicitud, HttpStatus.BAD_REQUEST.value());

		return responder(HttpStatus.BAD_REQUEST, mensajes);
	}

	@ExceptionHandler(SolicitudInvalidaException.class)
	public ResponseEntity<Map<String, List<String>>> solicitudInvalida(SolicitudInvalidaException excepcion,
			HttpServletRequest solicitud) {
		registrar("VALIDACION", excepcion.getMessage(), excepcion.getClass().getSimpleName(), solicitud,
				HttpStatus.BAD_REQUEST.value());

		return responder(HttpStatus.BAD_REQUEST, List.of(excepcion.getMessage()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, List<String>>> tipoInvalido(MethodArgumentTypeMismatchException excepcion,
			HttpServletRequest solicitud) {
		registrar("VALIDACION", "Parametro invalido: " + excepcion.getName(),
				excepcion.getClass().getSimpleName(), solicitud, HttpStatus.BAD_REQUEST.value());

		return responder(HttpStatus.BAD_REQUEST, List.of("El parametro " + excepcion.getName() + " no es valido"));
	}

	@ExceptionHandler(AutenticacionException.class)
	public ResponseEntity<Map<String, List<String>>> autenticacion(AutenticacionException excepcion,
			HttpServletRequest solicitud) {
		registrar("AUTENTICACION", excepcion.getMessage(), excepcion.getClass().getSimpleName(), solicitud,
				HttpStatus.UNAUTHORIZED.value());

		return responder(HttpStatus.UNAUTHORIZED, List.of(excepcion.getMessage()));
	}

	@ExceptionHandler(PermisoException.class)
	public ResponseEntity<Map<String, List<String>>> permiso(PermisoException excepcion,
			HttpServletRequest solicitud) {
		registrar("PERMISO", excepcion.getMessage(), excepcion.getClass().getSimpleName(), solicitud,
				HttpStatus.FORBIDDEN.value());

		return responder(HttpStatus.FORBIDDEN, List.of(excepcion.getMessage()));
	}

	@ExceptionHandler(ServicioNoDisponibleException.class)
	public ResponseEntity<Map<String, List<String>>> servicioNoDisponible(ServicioNoDisponibleException excepcion,
			HttpServletRequest solicitud) {
		registrar("SERVICIO_NO_DISPONIBLE", excepcion.getMessage(), excepcion.getClass().getSimpleName(), solicitud,
				HttpStatus.SERVICE_UNAVAILABLE.value());

		return responder(HttpStatus.SERVICE_UNAVAILABLE, List.of(excepcion.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, List<String>>> mensajeIlegible(HttpMessageNotReadableException excepcion,
			HttpServletRequest solicitud) {
		registrar("DESERIALIZACION", "Solicitud con formato inválido",
				excepcion.getClass().getSimpleName(), solicitud, HttpStatus.BAD_REQUEST.value());

		return responder(HttpStatus.BAD_REQUEST,
				List.of("Los datos enviados no tienen el formato correcto"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, List<String>>> errorInterno(Exception excepcion,
			HttpServletRequest solicitud) {
		registrar("ERROR_INTERNO", "Error interno al procesar la solicitud",
				excepcion.getClass().getSimpleName(), solicitud, HttpStatus.INTERNAL_SERVER_ERROR.value());

		return responder(HttpStatus.INTERNAL_SERVER_ERROR,
				List.of("No fue posible procesar la solicitud"));
	}

	private void registrar(String accion, String detalle, String error, HttpServletRequest solicitud, int estado) {
		logger.error(accion, detalle, error, solicitud.getRequestURI(), solicitud.getMethod(), estado,
				LocalDateTime.now(reloj));
	}

	private static ResponseEntity<Map<String, List<String>>> responder(HttpStatus estado, List<String> mensajes) {
		return ResponseEntity.status(estado).body(Map.of("mensajes", mensajes));
	}

}
