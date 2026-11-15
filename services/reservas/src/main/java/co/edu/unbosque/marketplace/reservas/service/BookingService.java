package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.BookingListDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// HU-23 Reserva inmediata: creación directa de reservas sin aprobación del anfitrión
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiciosExternos serviciosExternos;

    public BookingService(BookingRepository bookingRepository, ServiciosExternos serviciosExternos) {
        this.bookingRepository = bookingRepository;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-064, CA-065, CA-066: Confirmar reserva inmediata a partir de cotización
    @Transactional
    public BookingResponseDTO crearReserva(String autorizacion, String idempotencyKey, BookingRequestDTO solicitud) {
        // 1. Validar que el usuario sea HUÉSPED
        Long huespedId = serviciosExternos.verificarRolHuesped(autorizacion);

        // 2. Idempotencia: si ya existe una reserva con esta clave, se devuelve la existente
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existente = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent()) {
                return BookingResponseDTO.desde(existente.get());
            }
        }

        // 3. Obtener los detalles de la cotización
        Long publicacionId = solicitud.publicacionId();
        LocalDate fechaLlegada = solicitud.fechaLlegada();
        LocalDate fechaSalida = solicitud.fechaSalida();
        Integer cantidadHuespedes = solicitud.cantidadHuespedes();
        BigDecimal montoTotal = solicitud.montoTotal();

        if (publicacionId == null || fechaLlegada == null || fechaSalida == null
                || cantidadHuespedes == null || montoTotal == null) {
            CotizacionDTO cotizacion = serviciosExternos.consultarCotizacion(solicitud.cotizacionId());
            if (publicacionId == null) publicacionId = cotizacion.publicacionId();
            if (fechaLlegada == null) fechaLlegada = cotizacion.fechaLlegada();
            if (fechaSalida == null) fechaSalida = cotizacion.fechaSalida();
            if (cantidadHuespedes == null) cantidadHuespedes = cotizacion.cantidadHuespedes();
            if (montoTotal == null) montoTotal = cotizacion.total();
        }

        // Validaciones básicas de negocio
        if (publicacionId == null) {
            throw new FechasInvalidasException("La publicación es obligatoria");
        }
        if (fechaLlegada == null || fechaSalida == null || !fechaLlegada.isBefore(fechaSalida)) {
            throw new FechasInvalidasException("La fecha de llegada debe ser anterior a la de salida");
        }
        if (cantidadHuespedes == null || cantidadHuespedes <= 0) {
            cantidadHuespedes = 1;
        }
        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }

        // 4. CA-064 y CA-065: Volver a verificar disponibilidad con Disponibilidad
        boolean disponible = serviciosExternos.verificarDisponibilidad(publicacionId, fechaLlegada, fechaSalida);
        if (!disponible) {
            // CA-065: Si otro huésped reservó mientras yo cotizaba, se rechaza
            throw new ConflictoReservaException("El alojamiento ya no está disponible para las fechas seleccionadas");
        }

        // 5. CA-064: Crear reserva en estado CONFIRMADA sin requerir aprobación del anfitrión
        String codigoReserva = generarCodigoReserva();
        Booking reserva = new Booking(
                codigoReserva,
                publicacionId,
                huespedId,
                solicitud.cotizacionId(),
                fechaLlegada,
                fechaSalida,
                cantidadHuespedes,
                montoTotal,
                solicitud.metodoPago(),
                idempotencyKey,
                BookingStatus.CONFIRMADA,
                LocalDateTime.now()
        );
        reserva = bookingRepository.save(reserva);

        // 6. CA-066: Bloquear periodo en el servicio de Disponibilidad
        serviciosExternos.bloquearPeriodo(publicacionId, reserva.getId(), fechaLlegada, fechaSalida);

        // 7. CA-066: Generar las notificaciones correspondientes (al huésped y al anfitrión)
        serviciosExternos.notificarReservaConfirmada(huespedId, reserva.getId(), reserva.getCodigoReserva());

        Long anfitrionId = serviciosExternos.consultarAnfitrion(publicacionId);
        if (anfitrionId != null && !anfitrionId.equals(huespedId)) {
            serviciosExternos.notificarReservaConfirmada(anfitrionId, reserva.getId(), reserva.getCodigoReserva());
        }

        return BookingResponseDTO.desde(reserva);
    }

    @Transactional(readOnly = true)
    public BookingResponseDTO consultarPorId(Long id) {
        Booking reserva = bookingRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("La reserva no existe"));
        return BookingResponseDTO.desde(reserva);
    }

    @Transactional(readOnly = true)
    public BookingListDTO listar(BookingStatus estado) {
        List<Booking> lista = estado != null
                ? bookingRepository.findByEstado(estado)
                : bookingRepository.findAll();
        return new BookingListDTO(lista.stream().map(BookingResponseDTO::desde).toList());
    }

    private String generarCodigoReserva() {
        return "RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
