package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// HU-36: el administrador oculta reseñas que incumplen las políticas
@Service
public class ModeracionResenasService {

    private final ResenaRepository resenaRepository;
    private final Clock reloj;

    public ModeracionResenasService(ResenaRepository resenaRepository, Clock reloj) {
        this.resenaRepository = resenaRepository;
        this.reloj = reloj;
    }

    // CA-100 y CA-101. El id del administrador sale del token
    @Transactional
    public ResultadoOcultamiento ocultar(Long idAdministrador, Long resenaId, String motivo) {
        // Decisión 2
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.length() < Resena.LONGITUD_MINIMA_MOTIVO || texto.length() > Resena.LONGITUD_MAXIMA_MOTIVO) {
            throw new SolicitudInvalidaException("El motivo es obligatorio y debe tener entre 3 y 500 caracteres");
        }

        // Decisión 3
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new NoEncontradoException("La reseña no existe"));

        // Decisión 4: si ya estaba oculta se responde igual y se conserva el registro de la primera vez
        if (!resena.isVisible()) {
            return new ResultadoOcultamiento(resena, false);
        }

        // CA-100 y CA-101
        resena.ocultar(idAdministrador, texto, LocalDateTime.now(reloj));
        return new ResultadoOcultamiento(resenaRepository.save(resena), true);
    }
}
