package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// HU-33: reseñas visibles y calificación promedio de las publicaciones
@Service
public class ConsultaResenasService {

    private final ResenaRepository resenaRepository;

    public ConsultaResenasService(ResenaRepository resenaRepository) {
        this.resenaRepository = resenaRepository;
    }

    // CA-092 y CA-093. Decisión 5: no se consulta a Alojamientos; sin reseñas la lista va vacía
    @Transactional(readOnly = true)
    public ResenasPublicacion listarVisibles(Long publicacionId) {
        List<Resena> visibles = resenaRepository.findByPublicacionIdAndVisibleTrueOrderByFechaDescIdDesc(
                publicacionId);
        return new ResenasPublicacion(visibles, promedio(visibles));
    }

    // Decisión 3: promedio a 1 decimal; null si no hay reseñas visibles
    static Double promedio(List<Resena> visibles) {
        if (visibles.isEmpty()) {
            return null;
        }
        double suma = visibles.stream().mapToInt(Resena::getCalificacion).sum();
        return redondear(suma / visibles.size());
    }

    static Double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
