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

    // HU-23, HU-24, HU-25: Crear reserva inmediata con pago simulado (idempotente)
    // Rol requerido: HUESPED
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDTO> crearReserva(
            @RequestHeader("Authorization") String autorizacion,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequestDTO solicitud) {
        BookingResponseDTO reserva = bookingService.crearReserva(autorizacion, idempotencyKey, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(reserva);
    }

    // HU-26: Consultar estado de una reserva
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponseDTO> consultarReserva(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.consultarPorId(id));
    }

    // HU-26: Historial de reservas
    @GetMapping("/bookings")
    public ResponseEntity<BookingListDTO> listarReservas(
            @RequestParam(required = false) BookingStatus estado) {
        return ResponseEntity.ok(bookingService.listar(estado));
    }

    // HU-32: Operación interna para consultar una reserva
    @GetMapping("/internal/bookings/{id}")
    public ResponseEntity<BookingResponseDTO> consultarReservaInterna(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.consultarPorId(id));
    }
}
