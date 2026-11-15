package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// HU-23: reserva inmediata. La reserva queda CONFIRMADA sin aprobación del anfitrión.
@Service
public class BookingService {

    private static final String MENSAJE_NO_DISPONIBLE =
            "El alojamiento ya no está disponible para las fechas seleccionadas";

    private final BookingRepository bookingRepository;
    private final ServiciosExternos serviciosExternos;

    public BookingService(BookingRepository bookingRepository, ServiciosExternos serviciosExternos) {
        this.bookingRepository = bookingRepository;
        this.serviciosExternos = serviciosExternos;
    }

    // CA-064, CA-065 y CA-066
    @Transactional
    public BookingResponseDTO crearReserva(String autorizacion, String idempotencyKey, BookingRequestDTO solicitud) {
        // 1. Solo el huésped puede reservar
        Long huespedId = serviciosExternos.verificarRolHuesped(autorizacion);

        // 2. RF-026 y RN-015: un reintento con la misma clave devuelve la misma reserva
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new SolicitudInvalidaException("La clave de idempotencia es obligatoria");
        }
        Optional<Booking> existente = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            if (!existente.get().getHuespedId().equals(huespedId)) {
                throw new PermisoException("La clave de idempotencia ya fue usada por otro usuario");
            }
            return BookingResponseDTO.desde(existente.get());
        }

        // 3. Datos de la reserva: snapshot del request o consulta a Cotización
        Long publicacionId = solicitud.publicacionId();
        LocalDate fechaLlegada = solicitud.fechaLlegada();
        LocalDate fechaSalida = solicitud.fechaSalida();
        Integer cantidadHuespedes = solicitud.cantidadHuespedes();
        BigDecimal montoTotal = solicitud.montoTotal();

        if (publicacionId == null || fechaLlegada == null || fechaSalida == null
                || cantidadHuespedes == null || montoTotal == null) {
            CotizacionDTO cotizacion = serviciosExternos.consultarCotizacion(solicitud.cotizacionId());
            if (publicacionId == null) {
                publicacionId = cotizacion.publicacionId();
            }
            if (fechaLlegada == null) {
                fechaLlegada = cotizacion.fechaLlegada();
            }
            if (fechaSalida == null) {
                fechaSalida = cotizacion.fechaSalida();
            }
            if (cantidadHuespedes == null) {
                cantidadHuespedes = cotizacion.cantidadHuespedes();
            }
            if (montoTotal == null) {
                montoTotal = cotizacion.total();
            }
        }

        // 4. Validaciones de negocio
        validarDatos(publicacionId, fechaLlegada, fechaSalida, cantidadHuespedes, montoTotal);

        // 5. CA-064 y CA-065: volver a verificar la disponibilidad justo antes de confirmar
        if (!serviciosExternos.verificarDisponibilidad(publicacionId, fechaLlegada, fechaSalida)) {
            throw new ConflictoReservaException(MENSAJE_NO_DISPONIBLE);
        }

        // 6. CA-064: se confirma la reserva sin aprobación del anfitrión
        Booking reserva = new Booking(
                generarCodigoReserva(),
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
                LocalDateTime.now());
        reserva = bookingRepository.save(reserva);

        // 7. CA-066: el periodo queda bloqueado. Un 409 aquí revierte la reserva (carrera real).
        serviciosExternos.bloquearPeriodo(publicacionId, reserva.getId(), fechaLlegada, fechaSalida);

        // 8. CA-066: notificaciones al huésped y, best-effort, al anfitrión
        notificar(huespedId, reserva,
                "Tu reserva " + reserva.getCodigoReserva() + " fue confirmada del "
                        + fechaLlegada + " al " + fechaSalida);
        Long anfitrionId = serviciosExternos.consultarAnfitrion(publicacionId);
        if (anfitrionId != null && !anfitrionId.equals(huespedId)) {
            notificar(anfitrionId, reserva,
                    "Se confirmó la reserva " + reserva.getCodigoReserva() + " en tu publicación del "
                            + fechaLlegada + " al " + fechaSalida);
        }

        return BookingResponseDTO.desde(reserva);
    }

    private void notificar(Long usuarioId, Booking reserva, String mensaje) {
        serviciosExternos.notificarReservaConfirmada(
                usuarioId, reserva.getId(), reserva.getCodigoReserva(), mensaje);
    }

    private static void validarDatos(Long publicacionId, LocalDate fechaLlegada, LocalDate fechaSalida,
            Integer cantidadHuespedes, BigDecimal montoTotal) {
        if (publicacionId == null) {
            throw new SolicitudInvalidaException("La publicación es obligatoria");
        }
        if (fechaLlegada == null || fechaSalida == null) {
            throw new FechasInvalidasException("Las fechas de llegada y salida son obligatorias");
        }
        if (!fechaLlegada.isBefore(fechaSalida)) {
            throw new FechasInvalidasException("La fecha de llegada debe ser anterior a la de salida");
        }
        if (cantidadHuespedes == null || cantidadHuespedes <= 0) {
            throw new SolicitudInvalidaException("La cantidad de huéspedes debe ser mayor a 0");
        }
        if (montoTotal == null || montoTotal.signum() < 0) {
            throw new SolicitudInvalidaException("El monto total es obligatorio y no puede ser negativo");
        }
    }

    private static String generarCodigoReserva() {
        return "RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
