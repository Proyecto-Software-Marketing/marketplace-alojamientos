package co.edu.unbosque.marketplace.resenas.repository;

import co.edu.unbosque.marketplace.resenas.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    // CA-090: la reserva ya tiene una reseña
    boolean existsByReservaId(Long reservaId);

    // CA-092 y CA-093: solo las visibles de la publicación, de la más reciente a la más antigua
    // (si dos tienen la misma fecha, primero la registrada después)
    List<Resena> findByPublicacionIdAndVisibleTrueOrderByFechaDescIdDesc(Long publicacionId);

    // Decisión 6: promedio de las reseñas visibles de varias publicaciones (lo consume Búsqueda).
    // Las publicaciones sin reseñas visibles no salen en el resultado
    @Query("select r.publicacionId as publicacionId, avg(r.calificacion) as promedio from Resena r "
            + "where r.visible = true and r.publicacionId in :publicaciones "
            + "group by r.publicacionId order by r.publicacionId")
    List<PromedioPublicacion> promediarVisibles(@Param("publicaciones") List<Long> publicaciones);
}
