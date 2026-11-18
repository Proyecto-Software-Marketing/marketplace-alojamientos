package co.edu.unbosque.marketplace.alojamientos.repository;

import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    // Decisión 12: publicaciones de un estado, de la más antigua a la más nueva
    List<Publicacion> findByEstadoOrderByIdAsc(EstadoPublicacion estado);

    // Decisión 12: la ciudad se compara sin distinguir mayúsculas
    List<Publicacion> findByEstadoAndCiudadIgnoreCaseOrderByIdAsc(EstadoPublicacion estado, String ciudad);

    // Decisión 14: cuántas publicaciones hay en un estado
    long countByEstado(EstadoPublicacion estado);

    // Decisión 15: publicaciones de un anfitrión en un estado
    List<Publicacion> findByAnfitrionIdAndEstado(Long anfitrionId, EstadoPublicacion estado);

    // HU-15: consultar todas las publicaciones de un anfitrion.
    List<Publicacion> findByAnfitrionIdOrderByIdAsc(Long anfitrionId);
}
