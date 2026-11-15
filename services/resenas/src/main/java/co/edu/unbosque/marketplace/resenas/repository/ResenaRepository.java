package co.edu.unbosque.marketplace.resenas.repository;

import co.edu.unbosque.marketplace.resenas.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    // CA-090: la reserva ya tiene una reseña
    boolean existsByReservaId(Long reservaId);
}
