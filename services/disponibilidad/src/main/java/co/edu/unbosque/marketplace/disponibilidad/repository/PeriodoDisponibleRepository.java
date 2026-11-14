package co.edu.unbosque.marketplace.disponibilidad.repository;

import co.edu.unbosque.marketplace.disponibilidad.model.PeriodoDisponible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodoDisponibleRepository extends JpaRepository<PeriodoDisponible, Long> {

    // Periodos de una publicación ordenados por fecha (para consultarlos y para el calendario)
    List<PeriodoDisponible> findByIdPublicacionOrderByFechaInicioAsc(Long idPublicacion);

    // Busca el periodo solo si pertenece a esa publicación
    Optional<PeriodoDisponible> findByIdAndIdPublicacion(Long id, Long idPublicacion);

    // Decisión 2 de la HU-16: no se permiten periodos solapados en la misma publicación.
    // idExcluido sirve al editar, para que el periodo no choque consigo mismo (al crear se envía null)
    @Query("""
            SELECT COUNT(p) > 0 FROM PeriodoDisponible p
            WHERE p.idPublicacion = :idPublicacion
              AND (:idExcluido IS NULL OR p.id <> :idExcluido)
              AND p.fechaInicio < :fechaFin
              AND :fechaInicio < p.fechaFin
            """)
    boolean existeSolapado(@Param("idPublicacion") Long idPublicacion,
                           @Param("fechaInicio") LocalDate fechaInicio,
                           @Param("fechaFin") LocalDate fechaFin,
                           @Param("idExcluido") Long idExcluido);
}