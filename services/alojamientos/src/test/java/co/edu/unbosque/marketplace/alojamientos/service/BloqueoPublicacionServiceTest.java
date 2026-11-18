package co.edu.unbosque.marketplace.alojamientos.service;

import co.edu.unbosque.marketplace.alojamientos.exception.ConflictoException;
import co.edu.unbosque.marketplace.alojamientos.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.alojamientos.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.alojamientos.model.EstadoPublicacion;
import co.edu.unbosque.marketplace.alojamientos.model.Publicacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BloqueoPublicacionServiceTest {

    private PublicacionRepository repositorio;
    private BloqueoPublicacionService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(PublicacionRepository.class);
        servicio = new BloqueoPublicacionService(repositorio);
    }

    @Test
    void bloqueaPublicacionConMotivo() {
        Publicacion publicacion = nuevaPublicacion();

        when(repositorio.findById(1L))
                .thenReturn(Optional.of(publicacion));
        when(repositorio.save(any(Publicacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Publicacion resultado =
                servicio.bloquear(1L, "Incumple las reglas");

        assertEquals(EstadoPublicacion.BLOQUEADA, resultado.getEstado());
        assertEquals("Incumple las reglas", resultado.getMotivoBloqueo());
        verify(repositorio).save(publicacion);
    }

    @Test
    void rechazaMotivoCorto() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.bloquear(1L, "ab"));

        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaMotivoDemasiadoLargo() {
        String motivo = "a".repeat(2001);

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.bloquear(1L, motivo));

        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaPublicacionInexistente() {
        when(repositorio.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.bloquear(99L, "Motivo valido"));

        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaPublicacionYaBloqueada() {
        Publicacion publicacion = nuevaPublicacion();
        publicacion.bloquear("Motivo original");

        when(repositorio.findById(1L))
                .thenReturn(Optional.of(publicacion));

        assertThrows(ConflictoException.class,
                () -> servicio.bloquear(1L, "Nuevo motivo"));

        assertEquals("Motivo original", publicacion.getMotivoBloqueo());
        verify(repositorio, never()).save(any());
    }

    private Publicacion nuevaPublicacion() {
        return new Publicacion(10L, LocalDateTime.now());
    }
}