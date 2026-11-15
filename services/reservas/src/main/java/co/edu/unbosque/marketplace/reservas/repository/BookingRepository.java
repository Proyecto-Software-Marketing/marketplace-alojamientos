package co.edu.unbosque.marketplace.reservas.repository;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Optional<Booking> findByCodigoReserva(String codigoReserva);

    List<Booking> findByHuespedId(Long huespedId);

    List<Booking> findByPublicacionId(Long publicacionId);

    List<Booking> findByEstado(BookingStatus estado);

    List<Booking> findByHuespedIdAndEstado(Long huespedId, BookingStatus estado);

    List<Booking> findByPublicacionIdAndEstado(Long publicacionId, BookingStatus estado);
}
