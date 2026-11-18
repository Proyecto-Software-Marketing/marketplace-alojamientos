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

import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import java.util.ArrayList;
import java.util.List;
import co.edu.unbosque.marketplace.alojamientos.exception.PublicacionIncompletaException;

// HU-07: crear una publicaci├â┬│n en borrador
@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final Clock reloj;

    public PublicacionService(PublicacionRepository publicacionRepository, Clock reloj) {
        this.publicacionRepository = publicacionRepository;
        this.reloj = reloj;
    }

    // CA-020: la publicaci├â┬│n queda en BORRADOR asociada al anfitri├â┬│n (su id sale del token, decisi├â┬│n 5).
    // Decisi├â┬│n 9: no se valida t├â┬¡tulo repetido
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
    // HU-12: enviar una publicacion completa a revision.
@Transactional
public Publicacion enviarARevision(Long publicacionId, Long anfitrionId) {
    Publicacion publicacion = publicacionRepository.findById(publicacionId)
            .orElseThrow(() -> new NoEncontradoException(
                    "La publicacion no existe"));

    if (!publicacion.getAnfitrionId().equals(anfitrionId)) {
        throw new PermisoException(
                "No puede enviar a revision una publicacion de otro anfitrion");
    }

    if (publicacion.getEstado() != EstadoPublicacion.BORRADOR) {
        throw new ConflictoException(
                "Solo se pueden enviar a revision publicaciones en BORRADOR");
    }

    List<String> faltantes = new ArrayList<>();

    if (publicacion.getTitulo() == null
            || publicacion.getTitulo().isBlank()) {
        faltantes.add("titulo");
    }

    if (publicacion.getDescripcion() == null
            || publicacion.getDescripcion().isBlank()) {
        faltantes.add("descripcion");
    }

    if (publicacion.getCiudad() == null
            || publicacion.getCiudad().isBlank()) {
        faltantes.add("ciudad");
    }

    if (publicacion.getUbicacionTextual() == null
            || publicacion.getUbicacionTextual().isBlank()) {
        faltantes.add("ubicacion");
    }

    if (publicacion.getTipo() == null) {
        faltantes.add("tipo de alojamiento");
    }

    if (publicacion.getCapacidad() < 1
            || publicacion.getCapacidad() > 50) {
        faltantes.add("capacidad");
    }

    if (publicacion.getPrecioNoche() == null
            || publicacion.getPrecioNoche().signum() <= 0) {
        faltantes.add("precio por noche");
    }

if (!faltantes.isEmpty()) {
    throw new PublicacionIncompletaException(
            "Falta informacion obligatoria: "
                    + String.join(", ", faltantes));
}

    publicacion.enviarARevision();

    return publicacionRepository.save(publicacion);
}

    // HU-08: consultar una publicacion por su identificador.
    @Transactional(readOnly = true)
    public Publicacion consultarPorId(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException(
                        "La publicacion no existe"));
    }
    // HU-13: consultar publicaciones por estado.
    @Transactional(readOnly = true)
    public List<Publicacion> listarPorEstado(EstadoPublicacion estado) {
        if (estado == null) {
            return publicacionRepository.findAll();
        }
        return publicacionRepository.findByEstadoOrderByIdAsc(estado);
    }
    // CA-021: capacidad menor o igual a cero o precio no positivo ├óÔÇáÔÇÖ se rechaza
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

    // HU-13 (CA-037): aprobar una publicacion pendiente de revision.
    @Transactional
    public Publicacion aprobar(Long publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new NoEncontradoException(
                        "La publicacion no existe"));

        if (publicacion.getEstado() != EstadoPublicacion.PENDIENTE_REVISION) {
            throw new ConflictoException(
                    "Solo se pueden aprobar publicaciones en PENDIENTE_REVISION");
        }

        publicacion.aprobar();

        return publicacionRepository.save(publicacion);
    }
// HU-13 (CA-038): rechazar una publicacion con motivo obligatorio.
@Transactional
public Publicacion rechazar(Long publicacionId, String motivo) {
    Publicacion publicacion = publicacionRepository.findById(publicacionId)
            .orElseThrow(() -> new NoEncontradoException(
                    "La publicacion no existe"));

    if (publicacion.getEstado() != EstadoPublicacion.PENDIENTE_REVISION) {
        throw new ConflictoException(
                "Solo se pueden rechazar publicaciones en PENDIENTE_REVISION");
    }

    publicacion.rechazar(motivo);

    return publicacionRepository.save(publicacion);
}

    // HU-15: listar publicaciones del anfitrion.
    @Transactional(readOnly = true)
    public List<Publicacion> listarMisPublicaciones(Long anfitrionId) {
        return publicacionRepository.findByAnfitrionIdOrderByIdAsc(anfitrionId);
    }
}