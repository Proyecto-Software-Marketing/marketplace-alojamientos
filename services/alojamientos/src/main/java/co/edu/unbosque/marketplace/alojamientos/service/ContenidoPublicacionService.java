package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.CatalogoServicios;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// HU-09: servicios, reglas e imágenes de la publicación.
// Primero se validan los datos (400), luego que la publicación sea del anfitrión (404)
// y por último que su estado permita el cambio (409)
@Service
public class ContenidoPublicacionService {

    public static final String MENSAJE_SERVICIOS_OBLIGATORIOS = "La lista de servicios es obligatoria";
    public static final String MENSAJE_SERVICIO =
            "Cada servicio debe ser uno de: " + String.join(", ", CatalogoServicios.VALORES);

    public static final String MENSAJE_REGLAS_OBLIGATORIAS = "La lista de reglas es obligatoria";
    public static final String MENSAJE_REGLA_VACIA = "Las reglas no pueden estar vacías";
    public static final String MENSAJE_REGLA_LARGA = "Cada regla puede tener máximo 200 caracteres";
    public static final String MENSAJE_CANTIDAD_REGLAS = "Se pueden registrar máximo 20 reglas";

    public static final String MENSAJE_IMAGENES_OBLIGATORIAS = "La lista de imágenes es obligatoria";
    public static final String MENSAJE_IMAGEN =
            "Cada imagen debe ser una dirección que empiece por http://, https:// o / y no tenga espacios";
    public static final String MENSAJE_IMAGEN_LARGA = "Cada imagen puede tener máximo 500 caracteres";
    public static final String MENSAJE_CANTIDAD_IMAGENES = "Se pueden registrar máximo 10 imágenes";

    public static final String MENSAJE_NO_EXISTE = "La publicación no existe";
    public static final String MENSAJE_ESTADO =
            "Solo se pueden cambiar los servicios, reglas e imágenes de una publicación en BORRADOR, ACTIVA o PAUSADA";

    // Decisión 11: URL http(s) o ruta que empieza por /, con al menos un carácter después y sin espacios
    private static final Pattern FORMATO_IMAGEN = Pattern.compile("^(https?://|/)\\S+$");

    private final PublicacionRepository publicacionRepository;

    public ContenidoPublicacionService(PublicacionRepository publicacionRepository) {
        this.publicacionRepository = publicacionRepository;
    }

    // CA-026: agregar o quitar servicios. La lista enviada reemplaza a la anterior (decisión 2)
    @Transactional
    public Publicacion definirServicios(Long anfitrionId, Long publicacionId, List<String> servicios) {
        List<String> normalizados = normalizarServicios(servicios);
        Publicacion publicacion = buscarEditable(anfitrionId, publicacionId);
        publicacion.definirServicios(normalizados);
        return publicacionRepository.save(publicacion);
    }

    // CA-027: registrar las reglas del alojamiento
    @Transactional
    public Publicacion definirReglas(Long anfitrionId, Long publicacionId, List<String> reglas) {
        List<String> limpias = normalizarReglas(reglas);
        Publicacion publicacion = buscarEditable(anfitrionId, publicacionId);
        publicacion.definirReglas(limpias);
        return publicacionRepository.save(publicacion);
    }

    // CA-028: agregar rutas o referencias de imágenes
    @Transactional
    public Publicacion definirImagenes(Long anfitrionId, Long publicacionId, List<String> imagenes) {
        List<String> limpias = normalizarImagenes(imagenes);
        Publicacion publicacion = buscarEditable(anfitrionId, publicacionId);
        publicacion.definirImagenes(limpias);
        return publicacionRepository.save(publicacion);
    }

    // Decisiones 5 a 7: solo valores del catálogo, con su escritura, sin repetidos y en el orden del catálogo
    private List<String> normalizarServicios(List<String> servicios) {
        if (servicios == null) {
            throw new SolicitudInvalidaException(MENSAJE_SERVICIOS_OBLIGATORIOS);
        }
        Set<String> sinRepetir = new LinkedHashSet<>();
        for (String servicio : servicios) {
            Optional<String> delCatalogo = CatalogoServicios.normalizar(servicio);
            if (delCatalogo.isEmpty()) {
                throw new SolicitudInvalidaException(MENSAJE_SERVICIO);
            }
            sinRepetir.add(delCatalogo.get());
        }
        List<String> ordenados = new ArrayList<>(sinRepetir);
        ordenados.sort(Comparator.comparingInt(CatalogoServicios::posicion));
        return ordenados;
    }

    // Decisiones 8 y 9: texto libre sin espacios sobrantes, en el orden recibido y sin repetidas
    private List<String> normalizarReglas(List<String> reglas) {
        if (reglas == null) {
            throw new SolicitudInvalidaException(MENSAJE_REGLAS_OBLIGATORIAS);
        }
        Set<String> sinRepetir = new LinkedHashSet<>();
        for (String regla : reglas) {
            if (regla == null || regla.isBlank()) {
                throw new SolicitudInvalidaException(MENSAJE_REGLA_VACIA);
            }
            String limpia = regla.trim();
            if (limpia.length() > Publicacion.LONGITUD_MAXIMA_REGLA) {
                throw new SolicitudInvalidaException(MENSAJE_REGLA_LARGA);
            }
            sinRepetir.add(limpia);
        }
        if (sinRepetir.size() > Publicacion.CANTIDAD_MAXIMA_REGLAS) {
            throw new SolicitudInvalidaException(MENSAJE_CANTIDAD_REGLAS);
        }
        return new ArrayList<>(sinRepetir);
    }

    // Decisiones 10 a 12: solo referencias como texto, con formato mínimo, en el orden recibido y sin repetidas
    private List<String> normalizarImagenes(List<String> imagenes) {
        if (imagenes == null) {
            throw new SolicitudInvalidaException(MENSAJE_IMAGENES_OBLIGATORIAS);
        }
        Set<String> sinRepetir = new LinkedHashSet<>();
        for (String imagen : imagenes) {
            String limpia = imagen == null ? "" : imagen.trim();
            if (!FORMATO_IMAGEN.matcher(limpia).matches()) {
                throw new SolicitudInvalidaException(MENSAJE_IMAGEN);
            }
            if (limpia.length() > Publicacion.LONGITUD_MAXIMA_IMAGEN) {
                throw new SolicitudInvalidaException(MENSAJE_IMAGEN_LARGA);
            }
            sinRepetir.add(limpia);
        }
        if (sinRepetir.size() > Publicacion.CANTIDAD_MAXIMA_IMAGENES) {
            throw new SolicitudInvalidaException(MENSAJE_CANTIDAD_IMAGENES);
        }
        return new ArrayList<>(sinRepetir);
    }

    // Decisión 3: la publicación de otro anfitrión responde igual que una que no existe (404).
    // Decisión 4: solo en BORRADOR, ACTIVA o PAUSADA (409)
    private Publicacion buscarEditable(Long anfitrionId, Long publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .filter(encontrada -> encontrada.getAnfitrionId().equals(anfitrionId))
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_NO_EXISTE));
        if (!publicacion.permiteEditarContenido()) {
            throw new ConflictoException(MENSAJE_ESTADO);
        }
        return publicacion;
    }
}
