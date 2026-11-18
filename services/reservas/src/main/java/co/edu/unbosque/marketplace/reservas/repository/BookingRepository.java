package co.edu.unbosque.marketplace.reservas.repository;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // RF-026 y RN-015: un reintento con la misma clave devuelve la misma reserva
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Optional<Booking> findByCodigoReserva(String codigoReserva);

    // CA-068: el huésped consulta solo sus reservas, de la más reciente a la más antigua
    List<Booking> findByHuespedIdOrderByCreadaEnDesc(Long huespedId);

    List<Booking> findByHuespedIdAndEstadoOrderByCreadaEnDesc(Long huespedId, BookingStatus estado);

    // HU-26: el anfitrión consulta las reservas de sus publicaciones
    List<Booking> findByAnfitrionIdOrderByCreadaEnDesc(Long anfitrionId);

    List<Booking> findByAnfitrionIdAndEstadoOrderByCreadaEnDesc(Long anfitrionId, BookingStatus estado);

    // HU-27: reservas en un estado cuya fecha de salida es anterior a la fecha dada
    List<Booking> findByEstadoAndFechaSalidaBefore(BookingStatus estado, LocalDate fecha);
    
    // si el huesped tiene reservas en ese estado con llegada despues de la fecha
    boolean existsByHuespedIdAndEstadoAndFechaLlegadaAfter(Long huespedId, BookingStatus estado, LocalDate fecha);
    List<Booking> findByPublicacionIdAndEstadoAndFechaSalidaAfter(
            Long publicacionId,
            BookingStatus estado,
            LocalDate fecha);
}
