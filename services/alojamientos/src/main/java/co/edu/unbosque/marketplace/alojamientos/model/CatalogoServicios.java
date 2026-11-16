package co.edu.unbosque.marketplace.alojamientos.model;

import java.util.List;
import java.util.Optional;

// HU-09, decisión 5: catálogo fijo de servicios (amenities). Son los mismos que ofrece
// la pantalla de búsqueda como filtro, para que CA-026 funcione: Búsqueda compara los textos exactos
public final class CatalogoServicios {

    public static final List<String> VALORES =
            List.of("Wifi", "Cocina", "Aire acondicionado", "Lavadora", "Aparcamiento", "Piscina");

    private CatalogoServicios() {
    }

    // Decisión 6: acepta el servicio sin distinguir mayúsculas ni espacios sobrantes
    // y lo devuelve con la escritura del catálogo ("wifi" → "Wifi"). Si no está, vacío
    public static Optional<String> normalizar(String servicio) {
        if (servicio == null) {
            return Optional.empty();
        }
        String limpio = servicio.trim();
        return VALORES.stream().filter(valor -> valor.equalsIgnoreCase(limpio)).findFirst();
    }

    // Decisión 7: posición en el catálogo, para guardar la lista en ese orden
    public static int posicion(String servicio) {
        return VALORES.indexOf(servicio);
    }
}
