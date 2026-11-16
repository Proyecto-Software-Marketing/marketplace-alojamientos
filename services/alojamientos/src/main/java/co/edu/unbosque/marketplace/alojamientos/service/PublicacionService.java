package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

// HU-07: crear una publicación en borrador
@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final Clock reloj;

    public PublicacionService(PublicacionRepository publicacionRepository, Clock reloj) {
        this.publicacionRepository = publicacionRepository;
        this.reloj = reloj;
    }

    // CA-020: la publicación queda en BORRADOR asociada al anfitrión (su id sale del token, decisión 5).
    // Decisión 9: no se valida título repetido
    @Transactional
    public Publicacion crear(Long anfitrionId, PublicacionRequestDTO solicitud) {
        validarCapacidadYPrecio(solicitud);

        Publicacion publicacion = new Publicacion(anfitrionId, LocalDateTime.now(reloj));
        publicacion.definirDatosBasicos(
                solicitud.titulo().trim(),
                solicitud.descripcion().trim(),
                solicitud.ciudad().trim(),
                solicitud.ubicacionTextual().trim(),
                solicitud.tipo(),
                solicitud.capacidad().intValueExact(),
                // 1.5E+5 y 150000 son el mismo precio: se guarda siempre sin exponente
                solicitud.precioPorNoche().setScale(0, RoundingMode.UNNECESSARY));
        return publicacionRepository.save(publicacion);
    }

    // CA-021: capacidad menor o igual a cero o precio no positivo → se rechaza
    private void validarCapacidadYPrecio(PublicacionRequestDTO solicitud) {
        BigDecimal capacidad = solicitud.capacidad();
        if (capacidad == null || capacidad.signum() <= 0) {
            throw new SolicitudInvalidaException(PublicacionRequestDTO.MENSAJE_CAPACIDAD);
        }
        BigDecimal precio = solicitud.precioPorNoche();
        if (precio == null || precio.signum() <= 0) {
            throw new SolicitudInvalidaException(PublicacionRequestDTO.MENSAJE_PRECIO);
        }
    }
}
