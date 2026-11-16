package co.edu.unbosque.marketplace.auditoria.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SanitizadorDatosSensibles {

	public static final int LONGITUD_MAXIMA_DETALLE = 1000;

	private static final String CLAVES_SENSIBLES = "password|contrasena|contraseña|token|authorization|secret|"
			+ "secreto|tarjeta|card|cvv|cvc";

	private static final Pattern VALOR_ENTRE_COMILLAS = Pattern.compile(
			"(?iu)([\"']?(?:" + CLAVES_SENSIBLES + ")[\"']?\\s*[:=]\\s*)([\"'])(.*?)\\2");

	private static final Pattern VALOR_SIN_COMILLAS = Pattern.compile(
			"(?iu)([\"']?(?:" + CLAVES_SENSIBLES + ")[\"']?\\s*[:=]\\s*)([^,;}]+)");

	private static final Pattern CREDENCIAL_HTTP = Pattern.compile(
			"(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]+");

	public String limpiar(String detalle) {
		if (detalle == null) {
			return null;
		}

		String sinControles = detalle.replaceAll("\\p{Cntrl}", " ");
		String sinValoresEntreComillas = VALOR_ENTRE_COMILLAS.matcher(sinControles).replaceAll("$1***");
		String sinValores = VALOR_SIN_COMILLAS.matcher(sinValoresEntreComillas).replaceAll("$1***");
		String sinCredencialesHttp = CREDENCIAL_HTTP.matcher(sinValores).replaceAll("$1 ***");
		String limpio = sinCredencialesHttp.trim();

		if (limpio.isEmpty()) {
			return null;
		}

		return limpio.length() > LONGITUD_MAXIMA_DETALLE
				? limpio.substring(0, LONGITUD_MAXIMA_DETALLE)
				: limpio;
	}

}
