package co.edu.unbosque.marketplace.disponibilidad.service;

import co.edu.unbosque.marketplace.disponibilidad.dto.TramoCalendarioDTO;
import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import co.edu.unbosque.marketplace.disponibilidad.model.EstadoCalendario;
import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import co.edu.unbosque.marketplace.disponibilidad.repository.BloqueoReservaRepository;
import co.edu.unbosque.marketplace.disponibilidad.repository.PeriodoDisponibleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// HU-17: arma el calendario combinando los periodos del anfitrión con las reservas confirmadas
@Service
public class CalendarioService {

    // Estado de la publicación en Alojamientos cuando un administrador la bloquea (decisión 1 de la HU-17)
    private static final String PUBLICACION_BLOQUEADA = "BLOQUEADA";

    private final PeriodoDisponibleRepository periodoRepository;
    private final BloqueoReservaRepository bloqueoRepository;

    public CalendarioService(PeriodoDisponibleRepository periodoRepository,
            BloqueoReservaRepository bloqueoRepository) {
        this.periodoRepository = periodoRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    // CA-048 tramos diferenciados; decisión 3: solo desde hoy
    @Transactional(readOnly = true)
    public List<TramoCalendarioDTO> construir(Long idPublicacion, String estadoPublicacion) {
        LocalDate hoy = LocalDate.now();
        boolean bloqueada = PUBLICACION_BLOQUEADA.equals(estadoPublicacion);
        List<TramoCalendarioDTO> tramos = new ArrayList<>();

        for (PeriodoDisponible periodo : periodoRepository.findByIdPublicacionOrderByFechaInicioAsc(idPublicacion)) {
            LocalDate inicio = mayor(periodo.getFechaInicio(), hoy);
            LocalDate fin = periodo.getFechaFin();

            if (bloqueada) {
                agregar(tramos, periodo.getId(), inicio, fin, EstadoCalendario.BLOQUEADO);
            } else {
                partirPorReservas(periodo, inicio, fin, tramos);
            }
        }
        return tramos;
    }

    // CA-049: cada reserva confirmada parte el periodo en tramos disponibles y reservados
    private void partirPorReservas(PeriodoDisponible periodo, LocalDate inicio, LocalDate fin,
            List<TramoCalendarioDTO> tramos) {
        List<BloqueoReserva> bloqueos = new ArrayList<>(
                bloqueoRepository.buscarSolapados(periodo.getIdPublicacion(), inicio, fin));
        bloqueos.sort(Comparator.comparing(BloqueoReserva::getFechaLlegada));

        LocalDate cursor = inicio;
        for (BloqueoReserva bloqueo : bloqueos) {
            LocalDate desde = mayor(bloqueo.getFechaLlegada(), cursor);
            LocalDate hasta = menor(bloqueo.getFechaSalida(), fin);

            agregar(tramos, periodo.getId(), cursor, desde, EstadoCalendario.DISPONIBLE);
            agregar(tramos, periodo.getId(), desde, hasta, EstadoCalendario.RESERVADO);
            cursor = mayor(cursor, hasta);
        }
        agregar(tramos, periodo.getId(), cursor, fin, EstadoCalendario.DISPONIBLE);
    }

    // Solo agrega tramos de al menos una noche
    private static void agregar(List<TramoCalendarioDTO> tramos, Long idPeriodo, LocalDate desde, LocalDate hasta,
            EstadoCalendario estado) {
        if (hasta.isAfter(desde)) {
            tramos.add(new TramoCalendarioDTO(idPeriodo, desde, hasta, estado));
        }
    }

    private static LocalDate mayor(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate menor(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}