package co.edu.unbosque.marketplace.reservas.client;

import java.time.LocalDate;

// Cuerpo de POST /internal/availability/block (BlockRequest en el contrato)
public record BloqueoRequestDTO(
        Long publicacionId,
        Long reservaId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida) {
}
