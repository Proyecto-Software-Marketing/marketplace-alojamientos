package co.edu.unbosque.marketplace.reservas.repository;

import co.edu.unbosque.marketplace.reservas.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // RF-026 y RN-015: un reintento con la misma clave devuelve la misma reserva
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Optional<Booking> findByCodigoReserva(String codigoReserva);
}
