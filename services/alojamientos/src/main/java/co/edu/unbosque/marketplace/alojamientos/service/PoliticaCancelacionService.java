package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.PermisoException;
import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PoliticaCancelacionService {

    private final PoliticaCancelacionRepository politicaRepository;
    private final PublicacionRepository publicacionRepository;

    public PoliticaCancelacionService(
            PoliticaCancelacionRepository politicaRepository,
            PublicacionRepository publicacionRepository) {
        this.politicaRepository = politicaRepository;
        this.publicacionRepository = publicacionRepository;
    }

    @Transactional(readOnly = true)
    public List<PoliticaCancelacion> listar() {
        return politicaRepository.findAll();
    }

    @Transactional
    public Publicacion asociar(Long publicacionId, Long anfitrionId,
            Long politicaId) {

        if (politicaId == null || politicaId <= 0) {
            throw new SolicitudInvalidaException(
                    "Debe seleccionar una politica de cancelacion valida");
        }

        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new NoEncontradoException(
                        "La publicacion no existe"));

        if (!publicacion.getAnfitrionId().equals(anfitrionId)) {
            throw new PermisoException(
                    "No puede modificar una publicacion de otro anfitrion");
        }

        if (!publicacion.permiteEditarContenido()) {
            throw new ConflictoException(
                    "El estado de la publicacion no permite modificar la politica");
        }

        PoliticaCancelacion politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "La politica de cancelacion seleccionada no existe"));

        publicacion.definirPoliticaCancelacion(politica);

        return publicacionRepository.save(publicacion);
    }

    @Transactional(readOnly = true)
    public PoliticaCancelacion consultarInterna(Long publicacionId) {

        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new NoEncontradoException(
                        "La publicacion no existe"));

        if (publicacion.getPoliticaCancelacion() == null) {
            throw new NoEncontradoException(
                    "La publicacion no tiene una politica de cancelacion asociada");
        }

        return publicacion.getPoliticaCancelacion();
    }
}