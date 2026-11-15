package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // HU-23: crear la reserva inmediata de forma idempotente. Rol requerido: HUESPED.
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDTO> crearReserva(
            @RequestHeader("Authorization") String autorizacion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BookingRequestDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.crearReserva(autorizacion, idempotencyKey, solicitud));
    }
}
