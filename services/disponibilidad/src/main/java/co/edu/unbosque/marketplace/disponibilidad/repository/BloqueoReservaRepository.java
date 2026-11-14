package co.edu.unbosque.marketplace.disponibilidad.repository;

import co.edu.unbosque.marketplace.disponibilidad.model.BloqueoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BloqueoReservaRepository extends JpaRepository<BloqueoReserva, Long> {

    // CA-047: reservas confirmadas que ocupan alguna noche del rango indicado
    @Query("""
            SELECT b FROM BloqueoReserva b
            WHERE b.idPublicacion = :idPublicacion
              AND b.fechaLlegada < :fechaFin
              AND :fechaInicio < b.fechaSalida
            """)
    List<BloqueoReserva> buscarSolapados(@Param("idPublicacion") Long idPublicacion,
                                         @Param("fechaInicio") LocalDate fechaInicio,
                                         @Param("fechaFin") LocalDate fechaFin);

    // HU-18: para la idempotencia (decisión 3) y para liberar (decisión 4)
    Optional<BloqueoReserva> findByIdReserva(Long idReserva);
}