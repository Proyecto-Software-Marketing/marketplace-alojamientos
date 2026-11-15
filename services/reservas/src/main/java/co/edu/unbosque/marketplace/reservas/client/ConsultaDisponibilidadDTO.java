package co.edu.unbosque.marketplace.reservas.client;

import java.time.LocalDate;
import java.util.List;

// Cuerpo de POST /internal/availability/check (AvailabilityCheck en el contrato)
public record ConsultaDisponibilidadDTO(
        List<Long> publicacionIds,
        LocalDate fechaLlegada,
        LocalDate fechaSalida) {
}
