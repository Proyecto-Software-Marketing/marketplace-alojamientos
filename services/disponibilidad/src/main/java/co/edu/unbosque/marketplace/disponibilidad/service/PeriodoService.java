package co.edu.unbosque.marketplace.disponibilidad.service;

import co.edu.unbosque.marketplace.disponibilidad.exception.ConflictoDisponibilidadException;
import co.edu.unbosque.marketplace.disponibilidad.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.disponibilidad.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Reglas de negocio de los periodos disponibles (HU-16).
// Aquí solo se validan las fechas; el permiso del anfitrión se valida antes, en el controlador.
@Service
public class PeriodoService {

    private final PeriodoDisponibleRepository periodoRepository;
    private final BloqueoReservaRepository bloqueoRepository;

    public PeriodoService(PeriodoDisponibleRepository periodoRepository,
            BloqueoReservaRepository bloqueoRepository) {
        this.periodoRepository = periodoRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    // CA-045 registrar un periodo con fechas válidas
    @Transactional
    public PeriodoDisponible registrar(Long idPublicacion, LocalDate fechaInicio, LocalDate fechaFin) {
        validarOrdenFechas(fechaInicio, fechaFin);
        validarNoPasado(fechaInicio);
        validarSinSolapamiento(idPublicacion, fechaInicio, fechaFin, null);

        return periodoRepository.save(new PeriodoDisponible(idPublicacion, fechaInicio, fechaFin));
    }

    // Editar las fechas de un periodo; CA-047 no se puede recortar si tiene reservas confirmadas
    @Transactional
    public PeriodoDisponible editar(Long idPublicacion, Long idPeriodo, LocalDate fechaInicio, LocalDate fechaFin) {
        PeriodoDisponible periodo = buscar(idPublicacion, idPeriodo);

        validarOrdenFechas(fechaInicio, fechaFin);
        // Solo se valida el pasado si se mueve el inicio: un periodo que ya empezó puede ampliarse
        if (!fechaInicio.equals(periodo.getFechaInicio())) {
            validarNoPasado(fechaInicio);
        }
        validarSinSolapamiento(idPublicacion, fechaInicio, fechaFin, idPeriodo);

        // CA-047: cada reserva que estaba dentro del periodo debe seguir dentro de las nuevas fechas
        for (BloqueoReserva bloqueo : bloqueoRepository.buscarSolapados(
                idPublicacion, periodo.getFechaInicio(), periodo.getFechaFin())) {
            LocalDate ocupadoDesde = mayor(bloqueo.getFechaLlegada(), periodo.getFechaInicio());
            LocalDate ocupadoHasta = menor(bloqueo.getFechaSalida(), periodo.getFechaFin());

            if (ocupadoDesde.isBefore(fechaInicio) || ocupadoHasta.isAfter(fechaFin)) {
                throw new ConflictoDisponibilidadException(
                        "No se puede recortar el periodo: hay una reserva confirmada del "
                                + bloqueo.getFechaLlegada() + " al " + bloqueo.getFechaSalida());
            }
        }

        periodo.actualizarFechas(fechaInicio, fechaFin);
        return periodoRepository.save(periodo);
    }

    // CA-047 no se puede eliminar un periodo con reservas confirmadas
    @Transactional
    public void eliminar(Long idPublicacion, Long idPeriodo) {
        PeriodoDisponible periodo = buscar(idPublicacion, idPeriodo);

        boolean tieneReservas = !bloqueoRepository.buscarSolapados(
                idPublicacion, periodo.getFechaInicio(), periodo.getFechaFin()).isEmpty();
        if (tieneReservas) {
            throw new ConflictoDisponibilidadException(
                    "No se puede eliminar el periodo porque tiene reservas confirmadas");
        }

        periodoRepository.delete(periodo);
    }

    private PeriodoDisponible buscar(Long idPublicacion, Long idPeriodo) {
        return periodoRepository.findByIdAndIdPublicacion(idPeriodo, idPublicacion)
                .orElseThrow(() -> new NoEncontradoException("El periodo no existe en esta publicación"));
    }

    // CA-046
    private void validarOrdenFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new FechasInvalidasException("La fecha de salida debe ser posterior a la fecha de llegada");
        }
    }

    // Decisión 3 de la HU-16
    private void validarNoPasado(LocalDate fechaInicio) {
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new FechasInvalidasException("El periodo no puede empezar en una fecha pasada");
        }
    }

    // Decisión 2 de la HU-16
    private void validarSinSolapamiento(Long idPublicacion, LocalDate fechaInicio, LocalDate fechaFin,
            Long idExcluido) {
        if (periodoRepository.existeSolapado(idPublicacion, fechaInicio, fechaFin, idExcluido)) {
            throw new ConflictoDisponibilidadException(
                    "Ya existe un periodo disponible que se cruza con esas fechas");
        }
    }

    private static LocalDate mayor(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate menor(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}