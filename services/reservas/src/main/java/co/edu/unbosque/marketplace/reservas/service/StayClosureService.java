package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.dto.StayClosureResultDTO;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// HU-27: cierre automático de estancias. Una reserva CONFIRMADA cuya fecha de salida
// ya pasó (anterior a hoy en America/Bogota) pasa a COMPLETADA para que el huésped pueda reseñar.
@Service
public class StayClosureService {

    private static final Logger log = LoggerFactory.getLogger(StayClosureService.class);

    private final BookingRepository bookingRepository;
    private final Clock reloj;

    public StayClosureService(BookingRepository bookingRepository, Clock reloj) {
        this.bookingRepository = bookingRepository;
        this.reloj = reloj;
    }

    // CA-075 y CA-076: solo las CONFIRMADA cambian; CANCELADA, EN_REVISION_MANUAL y
    // COMPLETADA se quedan como están. Es idempotente: una segunda ejecución no cambia nada.
    @Transactional
    public StayClosureResultDTO cerrarEstanciasTerminadas() {
        LocalDate hoy = LocalDate.now(reloj);
        List<Booking> terminadas = bookingRepository
                .findByEstadoAndFechaSalidaBefore(BookingStatus.CONFIRMADA, hoy);
        terminadas.forEach(reserva -> reserva.setEstado(BookingStatus.COMPLETADA));
        bookingRepository.saveAll(terminadas);
        log.info("Cierre de estancias del {}: {} reservas pasaron a COMPLETADA", hoy, terminadas.size());
        return new StayClosureResultDTO(terminadas.size());
    }
}
