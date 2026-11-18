package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BloqueoPublicacionService {

    private final PublicacionRepository repositorio;

    public BloqueoPublicacionService(PublicacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    // HU-34 (CA-094): bloquear una publicacion con motivo obligatorio.
    @Transactional
    public Publicacion bloquear(Long id, String motivo) {

        if (motivo == null || motivo.trim().length() < 3) {
            throw new SolicitudInvalidaException(
                    "El motivo debe tener al menos 3 caracteres");
        }

        if (motivo.trim().length() > 2000) {
            throw new SolicitudInvalidaException(
                    "El motivo no puede superar 2000 caracteres");
        }

        Publicacion publicacion = repositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException(
                        "La publicacion no existe"));

        if (publicacion.getEstado() == EstadoPublicacion.BLOQUEADA) {
            throw new ConflictoException(
                    "La publicacion ya esta bloqueada");
        }

        publicacion.bloquear(motivo.trim());

        return repositorio.save(publicacion);
    }
}