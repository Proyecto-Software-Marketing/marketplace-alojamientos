package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lo que los otros servicios le piden a Alojamientos por las rutas internas
@Service
public class PublicacionInternaService {

    private final PublicacionRepository publicacionRepository;

    public PublicacionInternaService(PublicacionRepository publicacionRepository) {
        this.publicacionRepository = publicacionRepository;
    }

    // Decisiones 11 y 13: devuelve la publicación en cualquier estado; si no existe, 404
    public Publicacion buscar(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("La publicación no existe"));
    }

    // Decisión 12: publicaciones ACTIVA de una ciudad (sin distinguir mayúsculas); sin ciudad, todas las activas
    public List<Publicacion> activas(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return publicacionRepository.findByEstadoOrderByIdAsc(EstadoPublicacion.ACTIVA);
        }
        return publicacionRepository.findByEstadoAndCiudadIgnoreCaseOrderByIdAsc(EstadoPublicacion.ACTIVA,
                ciudad.trim());
    }

    // Decisión 14: cuántas publicaciones están ACTIVA
    public long contarActivas() {
        return publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA);
    }

    // Decisión 15: pasa a PAUSADA las publicaciones ACTIVA del anfitrión. Si no tiene ninguna no pasa nada,
    // así que Identidad puede repetir la llamada sin problema. Devuelve cuántas pausó
    @Transactional
    public int pausarPorAnfitrion(Long anfitrionId) {
        List<Publicacion> activas = publicacionRepository.findByAnfitrionIdAndEstado(anfitrionId,
                EstadoPublicacion.ACTIVA);
        activas.forEach(Publicacion::pausar);
        publicacionRepository.saveAll(activas);
        return activas.size();
    }
}
