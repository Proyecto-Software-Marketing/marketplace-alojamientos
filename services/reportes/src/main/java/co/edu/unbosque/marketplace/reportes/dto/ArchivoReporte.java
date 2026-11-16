package co.edu.unbosque.marketplace.reportes.dto;

import java.util.Arrays;
import java.util.Objects;

public record ArchivoReporte(byte[] contenido, String nombre, String contentType) {

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ArchivoReporte(byte[] contenidoOtro, String nombreOtro, String contentTypeOtro))) {
            return false;
        }
        return Arrays.equals(contenido, contenidoOtro)
                && Objects.equals(nombre, nombreOtro)
                && Objects.equals(contentType, contentTypeOtro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(contenido), nombre, contentType);
    }

    @Override
    public String toString() {
        return "ArchivoReporte[nombre=" + nombre + ", contentType=" + contentType
                + ", contenido=" + Arrays.toString(contenido) + "]";
    }
}
