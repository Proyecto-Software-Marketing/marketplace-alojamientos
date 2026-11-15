package co.edu.unbosque.marketplace.resenas.dto;

// Lo que Reseñas usa de GET /bookings/{id} del servicio Reservas.
// Reservas envía más campos (fechas, monto, código...): se ignoran
public record ReservaDTO(Long id, Long publicacionId, Long huespedId, String estado) {
}
