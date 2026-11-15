package co.edu.unbosque.marketplace.resenas.repository;

import co.edu.unbosque.marketplace.resenas.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    // CA-090: la reserva ya tiene una reseña
    boolean existsByReservaId(Long reservaId);

    // CA-092 y CA-093: solo las visibles de la publicación, de la más reciente a la más antigua
    // (si dos tienen la misma fecha, primero la registrada después)
    List<Resena> findByPublicacionIdAndVisibleTrueOrderByFechaDescIdDesc(Long publicacionId);
}
