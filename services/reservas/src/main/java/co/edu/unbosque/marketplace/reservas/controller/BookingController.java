package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.dto.BookingListDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // HU-23 y HU-24: crear la reserva inmediata de forma idempotente. Rol requerido: HUESPED.
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDTO> crearReserva(
            @RequestHeader("Authorization") String autorizacion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BookingRequestDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.crearReserva(autorizacion, idempotencyKey, solicitud));
    }

    // HU-26: historial de reservas. El huésped ve las suyas y el anfitrión las de sus publicaciones.
    @GetMapping("/bookings")
    public ResponseEntity<BookingListDTO> listarReservas(
            @RequestHeader("Authorization") String autorizacion,
            @RequestParam(required = false) BookingStatus estado) {
        return ResponseEntity.ok(bookingService.listarReservas(autorizacion, estado));
    }

    // HU-26: estado y detalle de una reserva. Solo su huésped o el anfitrión de la publicación.
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponseDTO> consultarReserva(
            @RequestHeader("Authorization") String autorizacion,
            @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.consultarReserva(autorizacion, id));
    }
}
