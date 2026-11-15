package co.edu.unbosque.marketplace.reservas.client;

import java.time.LocalDate;

public record BloqueoRequestDTO(
        Long publicacionId,
        Long reservaId,
        LocalDate fechaLlegada,
        LocalDate fechaSalida) {
}
