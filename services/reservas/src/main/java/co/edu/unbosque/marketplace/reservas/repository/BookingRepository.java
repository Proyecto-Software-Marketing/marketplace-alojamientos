package co.edu.unbosque.marketplace.reservas.repository;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
