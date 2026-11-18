package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.dto.PublicacionRequestDTO;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.alojamientos.repository.PoliticaCancelacionRepository;
import org.springframework.data.domain.Sort;
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

@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final PoliticaCancelacionRepository politicaRepository;
    private final Clock reloj;

    public PublicacionService(PublicacionRepository publicacionRepository,
            PoliticaCancelacionRepository politicaRepository, Clock reloj) {
        this.publicacionRepository = publicacionRepository;
        this.politicaRepository = politicaRepository;
        this.reloj = reloj;
    }


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
                politicaRepository.findAll(Sort.by("id")).stream().findFirst()
                .ifPresent(publicacion::definirPoliticaCancelacion);
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
    if (publicacion.getServicios().isEmpty()) {
        faltantes.add("servicios");
    }

    if (publicacion.getReglas().isEmpty()) {
        faltantes.add("reglas");
    }

    if (publicacion.getImagenes().isEmpty()) {
        faltantes.add("imagenes");
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
// HU-14 (CA-040): pausar una publicacion activa.
@Transactional
public Publicacion pausar(Long publicacionId, Long anfitrionId) {
    Publicacion publicacion = publicacionRepository.findById(publicacionId)
            .orElseThrow(() -> new NoEncontradoException(
                    "La publicacion no existe"));

    if (!publicacion.getAnfitrionId().equals(anfitrionId)) {
        throw new PermisoException(
                "No puede pausar una publicacion de otro anfitrion");
    }

    if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
        throw new ConflictoException(
                "Solo se pueden pausar publicaciones en estado ACTIVA");
    }

    publicacion.pausar();

    return publicacionRepository.save(publicacion);
}

// HU-14 (CA-041 y CA-042): reactivar una publicacion pausada.
@Transactional
public Publicacion reactivar(Long publicacionId, Long anfitrionId) {
    Publicacion publicacion = publicacionRepository.findById(publicacionId)
            .orElseThrow(() -> new NoEncontradoException(
                    "La publicacion no existe"));

    if (!publicacion.getAnfitrionId().equals(anfitrionId)) {
        throw new PermisoException(
                "No puede reactivar una publicacion de otro anfitrion");
    }

    if (publicacion.getEstado() != EstadoPublicacion.PAUSADA) {
        throw new ConflictoException(
                "Solo se pueden reactivar publicaciones en estado PAUSADA");
    }

    publicacion.reactivar();

    return publicacionRepository.save(publicacion);
}

    // HU-15: listar publicaciones del anfitrion.
    @Transactional(readOnly = true)
    public List<Publicacion> listarMisPublicaciones(Long anfitrionId) {
        return publicacionRepository.findByAnfitrionIdOrderByIdAsc(anfitrionId);
    }

    // HU-08: editar una publicacion existente.
    @Transactional
    public Publicacion editar(
            Long publicacionId,
            Long usuarioId,
            boolean esAdministrador,
            PublicacionRequestDTO solicitud) {

        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new NoEncontradoException(
                        "La publicacion no existe"));

        // El anfitrion solo puede editar sus propias publicaciones.
        if (!esAdministrador
                && !publicacion.getAnfitrionId().equals(usuarioId)) {
            throw new PermisoException(
                    "No puede editar una publicacion que no le pertenece");
        }

        // Verificar si el estado permite editar.
        if (!publicacion.permiteEditarContenido()) {
            throw new ConflictoException(
                    "La publicacion no permite editar su contenido en este estado");
        }

        validarCapacidadYPrecio(solicitud);

        publicacion.definirDatosBasicos(
                solicitud.titulo().trim(),
                solicitud.descripcion().trim(),
                solicitud.ciudad().trim(),
                solicitud.ubicacionTextual().trim(),
                solicitud.tipo(),
                solicitud.capacidad().intValueExact(),
                solicitud.precioPorNoche().setScale(
                        0, RoundingMode.UNNECESSARY));

        return publicacionRepository.save(publicacion);
    }
}
