package co.edu.unbosque.marketplace.reservas.client;

import java.time.LocalDate;
import java.util.List;

public record ConsultaDisponibilidadDTO(
        List<Long> publicacionIds,
        LocalDate fechaLlegada,
        LocalDate fechaSalida) {
}
