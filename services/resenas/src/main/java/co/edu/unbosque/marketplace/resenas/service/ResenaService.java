package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.dto.ReservaDTO;
import co.edu.unbosque.marketplace.resenas.exception.ConflictoResenaException;
import co.edu.unbosque.marketplace.resenas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.resenas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.resenas.model.Resena;
import co.edu.unbosque.marketplace.resenas.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// HU-32: el huésped califica y reseña una estancia completada
@Service
public class ResenaService {

    private static final String ESTADO_COMPLETADA = "COMPLETADA";

    private final ResenaRepository resenaRepository;
    private final Clock reloj;

    public ResenaService(ResenaRepository resenaRepository, Clock reloj) {
        this.resenaRepository = resenaRepository;
        this.reloj = reloj;
    }

    // CA-088 a CA-091. La reserva llega consultada en Reservas; el id del huésped sale del token
    @Transactional
    public Resena registrar(Long idHuesped, ReservaDTO reserva, Integer calificacion, String contenido) {
        // CA-091 y decisión 1
        if (calificacion == null || calificacion < Resena.CALIFICACION_MINIMA
                || calificacion > Resena.CALIFICACION_MAXIMA) {
            throw new SolicitudInvalidaException("La calificación debe ser un número entero entre 1 y 5");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new SolicitudInvalidaException("El contenido de la reseña es obligatorio");
        }
        String texto = contenido.trim();
        if (texto.length() > Resena.LONGITUD_MAXIMA_CONTENIDO) {
            throw new SolicitudInvalidaException("El contenido de la reseña no puede superar 1000 caracteres");
        }

        // Decisión 2: la reserva de otro huésped responde igual que una que no existe
        if (!idHuesped.equals(reserva.huespedId())) {
            throw new NoEncontradoException("La reserva no existe");
        }

        // CA-089 y decisión 3
        if (!ESTADO_COMPLETADA.equals(reserva.estado())) {
            throw new ConflictoResenaException("Solo se puede reseñar una estancia completada");
        }

        // CA-090 y decisión 4
        if (resenaRepository.existsByReservaId(reserva.id())) {
            throw new ConflictoResenaException("Esta reserva ya tiene una reseña");
        }

        // CA-088 y decisión 5: queda visible en la publicación de la reserva
        Resena nueva = new Resena(reserva.id(), reserva.publicacionId(), idHuesped, calificacion, texto,
                LocalDateTime.now(reloj));
        return resenaRepository.save(nueva);
    }
}
