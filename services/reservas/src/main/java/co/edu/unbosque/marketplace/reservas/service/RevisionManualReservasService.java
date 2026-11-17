package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class RevisionManualReservasService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Bogota");

    private final BookingRepository bookingRepository;

    public RevisionManualReservasService(
            BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // CA-095: revisar reservas afectadas por el bloqueo de una publicacion.
    @Transactional
    public int revisarReservas(Long publicacionId) {

        if (publicacionId == null || publicacionId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la publicacion no es valido");
        }

        LocalDate hoy = LocalDate.now(ZONA_HORARIA);

        List<Booking> reservas =
                bookingRepository.findByPublicacionIdAndEstadoAndFechaSalidaAfter(
                        publicacionId,
                        BookingStatus.CONFIRMADA,
                        hoy);

        for (Booking reserva : reservas) {
            reserva.setEstado(BookingStatus.EN_REVISION_MANUAL);
        }

        bookingRepository.saveAll(reservas);

        return reservas.size();
    }
}