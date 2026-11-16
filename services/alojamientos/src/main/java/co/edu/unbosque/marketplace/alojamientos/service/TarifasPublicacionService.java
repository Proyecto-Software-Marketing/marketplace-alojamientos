package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.alojamientos.dto.TarifasRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.model.ServicioAdicional;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// HU-10: tarifa de limpieza, descuento por estancia larga y servicios adicionales.
// Primero se validan los datos (400), luego que la publicación sea del anfitrión (404)
// y por último que su estado permita el cambio (409), igual que en la HU-09
@Service
public class TarifasPublicacionService {

    public static final String MENSAJE_NOCHES_OBLIGATORIAS =
            "Indique las noches mínimas desde las que se aplica el descuento";
    public static final String MENSAJE_NOCHES_SIN_DESCUENTO =
            "Sin porcentaje de descuento no se definen noches mínimas";
    public static final String MENSAJE_ADICIONAL_REPETIDO =
            "No puede haber dos servicios adicionales con el mismo nombre";
    public static final String MENSAJE_ESTADO =
            "Solo se pueden cambiar las tarifas de una publicación en BORRADOR, ACTIVA o PAUSADA";

    private final PublicacionRepository publicacionRepository;

    public TarifasPublicacionService(PublicacionRepository publicacionRepository) {
        this.publicacionRepository = publicacionRepository;
    }

    // CA-029, CA-030 y CA-129: guarda la configuración de precios que usará Cotización
    @Transactional
    public Publicacion definirTarifas(Long anfitrionId, Long publicacionId, TarifasRequestDTO solicitud) {
        BigDecimal limpieza = enteroValido(solicitud.tarifaLimpieza(), BigDecimal.ZERO,
                TarifasRequestDTO.MENSAJE_LIMPIEZA);
        int porcentaje = enteroValido(solicitud.porcentajeDescuentoEstanciaLarga(), BigDecimal.ZERO,
                TarifasRequestDTO.MENSAJE_PORCENTAJE).intValueExact();
        if (porcentaje > Publicacion.PORCENTAJE_MAXIMO_DESCUENTO) {
            throw new SolicitudInvalidaException(TarifasRequestDTO.MENSAJE_PORCENTAJE);
        }
        Integer noches = nochesMinimas(porcentaje, solicitud.nochesMinimasDescuento());
        List<ServicioAdicional> adicionales = serviciosAdicionales(solicitud.serviciosAdicionales());

        Publicacion publicacion = buscarEditable(anfitrionId, publicacionId);
        publicacion.definirTarifas(limpieza, porcentaje, noches, adicionales);
        return publicacionRepository.save(publicacion);
    }

    // CA-031: lo valida también el DTO; aquí se repite para cuando el servicio se usa directamente.
    // Devuelve el valor sin exponente ni decimales (1.5E+5 → 150000)
    private BigDecimal enteroValido(BigDecimal valor, BigDecimal minimo, String mensaje) {
        if (valor == null || valor.compareTo(minimo) < 0 || valor.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(mensaje);
        }
        return valor.setScale(0, RoundingMode.UNNECESSARY);
    }

    // Decisión 5: con descuento las noches mínimas son obligatorias; sin descuento deben ir en null
    private Integer nochesMinimas(int porcentaje, BigDecimal noches) {
        if (porcentaje == 0) {
            if (noches != null) {
                throw new SolicitudInvalidaException(MENSAJE_NOCHES_SIN_DESCUENTO);
            }
            return null;
        }
        if (noches == null) {
            throw new SolicitudInvalidaException(MENSAJE_NOCHES_OBLIGATORIAS);
        }
        int valor = enteroValido(noches, BigDecimal.valueOf(Publicacion.NOCHES_MINIMAS_DESCUENTO),
                TarifasRequestDTO.MENSAJE_NOCHES).intValueExact();
        if (valor > Publicacion.NOCHES_MAXIMAS_DESCUENTO) {
            throw new SolicitudInvalidaException(TarifasRequestDTO.MENSAJE_NOCHES);
        }
        return valor;
    }

    // Decisión 6: máximo 10, nombre sin espacios sobrantes, costo mayor que cero y sin nombres repetidos
    // (sin distinguir mayúsculas). Se conserva el orden recibido
    private List<ServicioAdicional> serviciosAdicionales(List<ServicioAdicionalDTO> recibidos) {
        if (recibidos == null) {
            throw new SolicitudInvalidaException(TarifasRequestDTO.MENSAJE_ADICIONALES);
        }
        if (recibidos.size() > Publicacion.CANTIDAD_MAXIMA_SERVICIOS_ADICIONALES) {
            throw new SolicitudInvalidaException(TarifasRequestDTO.MENSAJE_CANTIDAD_ADICIONALES);
        }
        List<ServicioAdicional> adicionales = new ArrayList<>();
        Set<String> nombres = new HashSet<>();
        for (ServicioAdicionalDTO recibido : recibidos) {
            if (recibido == null || recibido.nombre() == null || recibido.nombre().isBlank()
                    || recibido.nombre().trim().length() > ServicioAdicional.LONGITUD_MAXIMA_NOMBRE) {
                throw new SolicitudInvalidaException(ServicioAdicionalDTO.MENSAJE_NOMBRE);
            }
            String nombre = recibido.nombre().trim();
            BigDecimal costo = enteroValido(recibido.costo(), BigDecimal.ONE, ServicioAdicionalDTO.MENSAJE_COSTO);
            if (!nombres.add(nombre.toLowerCase(Locale.ROOT))) {
                throw new SolicitudInvalidaException(MENSAJE_ADICIONAL_REPETIDO);
            }
            adicionales.add(new ServicioAdicional(nombre, costo));
        }
        return adicionales;
    }

    // Decisión 2: la publicación de otro anfitrión responde como si no existiera (404);
    // solo en BORRADOR, ACTIVA o PAUSADA (409)
    private Publicacion buscarEditable(Long anfitrionId, Long publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .filter(encontrada -> encontrada.getAnfitrionId().equals(anfitrionId))
                .orElseThrow(() -> new NoEncontradoException(ContenidoPublicacionService.MENSAJE_NO_EXISTE));
        if (!publicacion.permiteEditarContenido()) {
            throw new ConflictoException(MENSAJE_ESTADO);
        }
        return publicacion;
    }
}
