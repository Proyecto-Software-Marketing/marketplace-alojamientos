package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.PromedioPublicacion;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// HU-33: reseñas visibles y calificación promedio de las publicaciones (también para Búsqueda)
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

    // Decisión 6: promedio de varias publicaciones, por ejemplo "1,2,3" → {1=4.5, 3=3.7}.
    // Las publicaciones sin reseñas visibles no aparecen
    @Transactional(readOnly = true)
    public Map<Long, Double> promedios(String idsSeparadosPorComa) {
        List<Long> publicaciones = leerIds(idsSeparadosPorComa);

        Map<Long, Double> promedios = new LinkedHashMap<>();
        for (PromedioPublicacion fila : resenaRepository.promediarVisibles(publicaciones)) {
            promedios.put(fila.getPublicacionId(), redondear(fila.getPromedio()));
        }
        return promedios;
    }

    // Decisión 6: si no envían ids o alguno no es número → 400
    static List<Long> leerIds(String idsSeparadosPorComa) {
        if (idsSeparadosPorComa == null || idsSeparadosPorComa.isBlank()) {
            throw new SolicitudInvalidaException("Debe enviar al menos un id de publicación en listingIds");
        }
        List<Long> ids = new ArrayList<>();
        for (String parte : idsSeparadosPorComa.split(",")) {
            try {
                ids.add(Long.parseLong(parte.trim()));
            } catch (NumberFormatException e) {
                throw new SolicitudInvalidaException("El id de publicación no es válido: '" + parte.trim() + "'");
            }
        }
        return ids;
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
