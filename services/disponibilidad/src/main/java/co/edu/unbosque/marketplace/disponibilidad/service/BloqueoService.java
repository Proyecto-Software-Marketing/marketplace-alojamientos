package co.edu.unbosque.marketplace.disponibilidad.service;

import co.edu.unbosque.marketplace.disponibilidad.exception.ConflictoDisponibilidadException;
import co.edu.unbosque.marketplace.disponibilidad.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// HU-18: bloqueo y liberación automática de periodos cuando Reservas confirma o cancela
@Service
public class BloqueoService {

    private final PeriodoDisponibleRepository periodoRepository;
    private final BloqueoReservaRepository bloqueoRepository;

    public BloqueoService(PeriodoDisponibleRepository periodoRepository,
            BloqueoReservaRepository bloqueoRepository) {
        this.periodoRepository = periodoRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    // CA-050: al confirmar una reserva, sus fechas quedan bloqueadas
    @Transactional
    public void bloquear(Long idPublicacion, Long idReserva, LocalDate llegada, LocalDate salida) {
        validarOrdenFechas(llegada, salida);

        // Decisión 5: primero el candado, para que la verificación y el guardado sean una sola operación
        List<PeriodoDisponible> periodos = periodoRepository.buscarQueCruzanParaBloquear(idPublicacion, llegada, salida);

        // Decisión 3: un reintento con los mismos datos no hace nada
        Optional<BloqueoReserva> existente = bloqueoRepository.findByIdReserva(idReserva);
        if (existente.isPresent()) {
            if (esIgual(existente.get(), idPublicacion, llegada, salida)) {
                return;
            }
            throw new ConflictoDisponibilidadException("La reserva ya tiene un bloqueo con otras fechas");
        }

        // Decisión 2 y CA-052
        if (!cubren(periodos, llegada, salida)) {
            throw new ConflictoDisponibilidadException(
                    "Las fechas no están dentro de los periodos disponibles de la publicación");
        }
        if (!bloqueoRepository.buscarSolapados(idPublicacion, llegada, salida).isEmpty()) {
            throw new ConflictoDisponibilidadException("Las fechas se cruzan con otra reserva confirmada");
        }

        bloqueoRepository.save(new BloqueoReserva(idPublicacion, idReserva, llegada, salida));
    }

    // Decisión 4: liberar es idempotente; si no hay bloqueo, no pasa nada
    @Transactional
    public void liberar(Long idReserva) {
        bloqueoRepository.findByIdReserva(idReserva).ifPresent(bloqueoRepository::delete);
    }

    // CA-052: de una lista de publicaciones, cuáles tienen libres esas fechas
    @Transactional(readOnly = true)
    public List<Long> disponibles(List<Long> idsPublicacion, LocalDate llegada, LocalDate salida) {
        validarOrdenFechas(llegada, salida);
        return idsPublicacion.stream()
                .filter(id -> estaDisponible(id, llegada, salida))
                .toList();
    }

    // Libre = cubierto por periodos disponibles y sin reservas que se crucen
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long idPublicacion, LocalDate llegada, LocalDate salida) {
        return cubren(periodoRepository.buscarQueCruzan(idPublicacion, llegada, salida), llegada, salida)
                && bloqueoRepository.buscarSolapados(idPublicacion, llegada, salida).isEmpty();
    }

    // Los periodos (ordenados) cubren de llegada a salida sin huecos, aunque sean varios contiguos
    private static boolean cubren(List<PeriodoDisponible> periodos, LocalDate llegada, LocalDate salida) {
        LocalDate cubiertoHasta = llegada;
        for (PeriodoDisponible periodo : periodos) {
            if (periodo.getFechaInicio().isAfter(cubiertoHasta)) {
                return false;
            }
            if (periodo.getFechaFin().isAfter(cubiertoHasta)) {
                cubiertoHasta = periodo.getFechaFin();
            }
        }
        return !cubiertoHasta.isBefore(salida);
    }

    private static boolean esIgual(BloqueoReserva bloqueo, Long idPublicacion, LocalDate llegada, LocalDate salida) {
        return bloqueo.getIdPublicacion().equals(idPublicacion)
                && bloqueo.getFechaLlegada().equals(llegada)
                && bloqueo.getFechaSalida().equals(salida);
    }

    private static void validarOrdenFechas(LocalDate llegada, LocalDate salida) {
        if (!salida.isAfter(llegada)) {
            throw new FechasInvalidasException("La fecha de salida debe ser posterior a la fecha de llegada");
        }
    }
}
