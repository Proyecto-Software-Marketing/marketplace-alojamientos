package co.edu.unbosque.marketplace.cotizacion.service;

import co.edu.unbosque.marketplace.cotizacion.client.ClienteCotizacion;
import co.edu.unbosque.marketplace.cotizacion.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.cotizacion.dto.DatosPublicacion;
import co.edu.unbosque.marketplace.cotizacion.dto.ServicioAdicionalCotizado;
import co.edu.unbosque.marketplace.cotizacion.exception.CapacidadExcedidaException;
import co.edu.unbosque.marketplace.cotizacion.exception.ConflictoException;
import co.edu.unbosque.marketplace.cotizacion.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.cotizacion.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.cotizacion.model.Cotizacion;
import co.edu.unbosque.marketplace.cotizacion.model.ServicioCotizado;
import co.edu.unbosque.marketplace.cotizacion.repository.CotizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// HU-22: cotización desglosada.
// total = noches × precio por noche + limpieza + servicios adicionales − descuento + tarifa de servicio (CA-061)
@Service
public class CotizacionService {

    public static final int NOCHES_MAXIMAS = 365;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ClienteCotizacion clienteCotizacion;
    private final TarifaService tarifaService;
    private final CotizacionRepository cotizacionRepository;
    private final Clock reloj;

    public CotizacionService(ClienteCotizacion clienteCotizacion, TarifaService tarifaService,
            CotizacionRepository cotizacionRepository, Clock reloj) {
        this.clienteCotizacion = clienteCotizacion;
        this.tarifaService = tarifaService;
        this.cotizacionRepository = cotizacionRepository;
        this.reloj = reloj;
    }

    // CA-060 a CA-063. El id del huésped sale del token (lo verifica el controlador)
    @Transactional
    public Cotizacion cotizar(Long idHuesped, CotizacionRequest datos) {
        int noches = validarFechas(datos.getFechaLlegada(), datos.getFechaSalida());

        DatosPublicacion publicacion = clienteCotizacion.consultarDatosPublicacion(datos.getPublicacionId());

        // Decisión 5: solo se cotizan publicaciones ACTIVA
        if (!"ACTIVA".equals(publicacion.getEstado())) {
            throw new ConflictoException("La publicación no está disponible para cotizar");
        }

        // CA-063
        if (datos.getCantidadHuespedes() > publicacion.getCapacidad()) {
            throw new CapacidadExcedidaException(
                    "La publicación admite máximo " + publicacion.getCapacidad() + " huéspedes");
        }

        List<ServicioCotizado> adicionales = serviciosElegidos(publicacion, datos.getServiciosAdicionales());

        // CA-061: cada parte se redondea al peso más cercano (decisión 9)
        BigDecimal precioPorNoche = pesos(publicacion.getPrecioPorNoche());
        BigDecimal precioBase = precioPorNoche.multiply(BigDecimal.valueOf(noches));
        BigDecimal limpieza = pesos(publicacion.getTarifaLimpieza());
        BigDecimal totalAdicionales = adicionales.stream()
                .map(ServicioCotizado::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // CA-062: el descuento aplica solo a las noches y si la estancia es larga
        int porcentajeDescuento = aplicaDescuento(publicacion, noches)
                ? publicacion.getPorcentajeDescuentoEstanciaLarga()
                : 0;
        BigDecimal descuento = porcentaje(precioBase, BigDecimal.valueOf(porcentajeDescuento));

        BigDecimal subtotal = precioBase.add(limpieza).add(totalAdicionales).subtract(descuento);

        // HU-38: el % de la tarifa de servicio que configuró el administrador, sobre el subtotal
        BigDecimal porcentajeServicio = tarifaService.consultarPorcentaje();
        BigDecimal tarifaServicio = porcentaje(subtotal, porcentajeServicio);
        BigDecimal total = subtotal.add(tarifaServicio);

        Cotizacion cotizacion = new Cotizacion(UUID.randomUUID().toString(), idHuesped, datos.getPublicacionId(),
                datos.getFechaLlegada(), datos.getFechaSalida(), datos.getCantidadHuespedes(),
                LocalDateTime.now(reloj));
        cotizacion.definirDesglose(noches, precioPorNoche, precioBase, limpieza, adicionales, porcentajeDescuento,
                descuento, porcentajeServicio, tarifaServicio, total);
        return cotizacionRepository.save(cotizacion);
    }

    // Decisión 1: Reservas la consulta con el id para reservar con el mismo precio
    public Cotizacion consultar(String id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("La cotización no existe"));
    }

    // Decisión 4: llegada desde hoy, salida después de la llegada y máximo 365 noches. Devuelve las noches
    private int validarFechas(LocalDate llegada, LocalDate salida) {
        if (llegada.isBefore(LocalDate.now(reloj))) {
            throw new SolicitudInvalidaException("La fecha de llegada no puede ser anterior a hoy");
        }
        if (!salida.isAfter(llegada)) {
            throw new SolicitudInvalidaException("La fecha de salida debe ser posterior a la de llegada");
        }
        long noches = ChronoUnit.DAYS.between(llegada, salida);
        if (noches > NOCHES_MAXIMAS) {
            throw new SolicitudInvalidaException("La estancia puede ser de máximo 365 noches");
        }
        return (int) noches;
    }

    // CA-062: hay descuento, y las noches alcanzan el mínimo que definió el anfitrión
    private boolean aplicaDescuento(DatosPublicacion publicacion, int noches) {
        Integer porcentaje = publicacion.getPorcentajeDescuentoEstanciaLarga();
        Integer minimo = publicacion.getNochesMinimasDescuento();
        return porcentaje != null && porcentaje > 0 && minimo != null && noches >= minimo;
    }

    // Decisión 7 y CA-129: solo servicios que ofrece la publicación, sin distinguir mayúsculas;
    // los repetidos se cobran una vez. Se guardan con el nombre y el costo de la publicación
    private List<ServicioCotizado> serviciosElegidos(DatosPublicacion publicacion, List<String> elegidos) {
        List<ServicioCotizado> resultado = new ArrayList<>();
        Set<String> yaElegidos = new HashSet<>();
        for (String elegido : elegidos) {
            if (elegido == null || elegido.isBlank()) {
                throw new SolicitudInvalidaException("Los servicios adicionales no pueden estar vacíos");
            }
            String nombre = elegido.trim();
            ServicioAdicionalCotizado ofrecido = buscarOfrecido(publicacion, nombre)
                    .orElseThrow(() -> new SolicitudInvalidaException(
                            "La publicación no ofrece el servicio adicional: " + nombre));
            if (yaElegidos.add(ofrecido.getNombre().toLowerCase(Locale.ROOT))) {
                resultado.add(new ServicioCotizado(ofrecido.getNombre(), pesos(ofrecido.getCosto())));
            }
        }
        return resultado;
    }

    private Optional<ServicioAdicionalCotizado> buscarOfrecido(DatosPublicacion publicacion, String nombre) {
        return publicacion.getServiciosAdicionales().stream()
                .filter(servicio -> servicio.getNombre() != null && servicio.getNombre().equalsIgnoreCase(nombre))
                .findFirst();
    }

    // valor × porcentaje / 100, redondeado al peso más cercano
    private BigDecimal porcentaje(BigDecimal valor, BigDecimal porcentaje) {
        return valor.multiply(porcentaje).divide(CIEN, 0, RoundingMode.HALF_UP);
    }

    private BigDecimal pesos(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(0, RoundingMode.HALF_UP);
    }
}
