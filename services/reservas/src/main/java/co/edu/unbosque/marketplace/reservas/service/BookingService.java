package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.dto.BookingListDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// HU-23: reserva inmediata sin aprobación del anfitrión.
// HU-24: un reintento (incluso simultáneo) nunca crea dos reservas ni dos pagos.
@Service
public class BookingService {

    private static final String MENSAJE_NO_DISPONIBLE =
            "El alojamiento ya no está disponible para las fechas seleccionadas";

    private final BookingRepository bookingRepository;
    private final ServiciosExternos serviciosExternos;
    private final TransactionTemplate transaccion;

    public BookingService(BookingRepository bookingRepository, ServiciosExternos serviciosExternos,
            PlatformTransactionManager gestorTransacciones) {
        this.bookingRepository = bookingRepository;
        this.serviciosExternos = serviciosExternos;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
    }

    // CA-064, CA-065, CA-066 (HU-23) y CA-067 (HU-24)
    public BookingResponseDTO crearReserva(String autorizacion, String idempotencyKey, BookingRequestDTO solicitud) {
        // 1. Solo el huésped puede reservar
        Long huespedId = serviciosExternos.verificarRolHuesped(autorizacion);

        // 2. RF-026 y RN-015: un reintento con la misma clave devuelve la misma reserva
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new SolicitudInvalidaException("La clave de idempotencia es obligatoria");
        }
        Optional<Booking> existente = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return devolverExistente(existente.get(), huespedId);
        }

        // 3. Datos de la reserva: snapshot del request o consulta a Cotización
        DatosReserva datos = resolverDatos(solicitud);
        validarDatos(datos);

        // 4. CA-064 y CA-065: volver a verificar la disponibilidad justo antes de confirmar
        if (!serviciosExternos.verificarDisponibilidad(datos.publicacionId(), datos.fechaLlegada(),
                datos.fechaSalida())) {
            throw new ConflictoReservaException(MENSAJE_NO_DISPONIBLE);
        }

        // 5. CA-064: se confirma la reserva sin aprobación del anfitrión
        try {
            return transaccion.execute(estado -> confirmarReserva(huespedId, idempotencyKey, solicitud, datos));
        } catch (DataIntegrityViolationException e) {
            // CA-067: dos clics simultáneos con la misma clave. La otra transacción
            // ya hizo commit (el insert único espera su turno) y se devuelve su reserva.
            return bookingRepository.findByIdempotencyKey(idempotencyKey)
                    .map(ganadora -> devolverExistente(ganadora, huespedId))
                    .orElseThrow(() -> e);
        }
    }

    // CA-068: el huésped solo ve sus propias reservas
    public BookingListDTO listarReservas(String autorizacion, BookingStatus estado) {
        Long huespedId = serviciosExternos.verificarRolHuesped(autorizacion);

        List<Booking> reservas = estado == null
                ? bookingRepository.findByHuespedIdOrderByCreadaEnDesc(huespedId)
                : bookingRepository.findByHuespedIdAndEstadoOrderByCreadaEnDesc(huespedId, estado);

        return new BookingListDTO(reservas.stream().map(BookingResponseDTO::desde).toList());
    }

    // CA-067: una única ejecución confirma la reserva y sus efectos externos
    private BookingResponseDTO confirmarReserva(Long huespedId, String idempotencyKey,
            BookingRequestDTO solicitud, DatosReserva datos) {
        Booking reserva = new Booking(
                generarCodigoReserva(),
                datos.publicacionId(),
                huespedId,
                solicitud.cotizacionId(),
                datos.fechaLlegada(),
                datos.fechaSalida(),
                datos.cantidadHuespedes(),
                datos.montoTotal(),
                solicitud.metodoPago(),
                idempotencyKey,
                BookingStatus.CONFIRMADA,
                LocalDateTime.now());

        // El flush fuerza la clave única antes del bloqueo y las notificaciones:
        // si otro clic ganó la carrera, aquí se lanza DataIntegrityViolationException.
        reserva = bookingRepository.saveAndFlush(reserva);

        // CA-066: el periodo queda bloqueado. Un 409 aquí revierte la reserva (carrera real).
        serviciosExternos.bloquearPeriodo(datos.publicacionId(), reserva.getId(),
                datos.fechaLlegada(), datos.fechaSalida());

        // CA-066: notificaciones al huésped y, best-effort, al anfitrión
        notificar(huespedId, reserva,
                "Tu reserva " + reserva.getCodigoReserva() + " fue confirmada del "
                        + datos.fechaLlegada() + " al " + datos.fechaSalida());
        Long anfitrionId = serviciosExternos.consultarAnfitrion(datos.publicacionId());
        if (anfitrionId != null && !anfitrionId.equals(huespedId)) {
            notificar(anfitrionId, reserva,
                    "Se confirmó la reserva " + reserva.getCodigoReserva() + " en tu publicación del "
                            + datos.fechaLlegada() + " al " + datos.fechaSalida());
        }

        return BookingResponseDTO.desde(reserva);
    }

    private BookingResponseDTO devolverExistente(Booking reserva, Long huespedId) {
        if (!reserva.getHuespedId().equals(huespedId)) {
            throw new PermisoException("La clave de idempotencia ya fue usada por otro usuario");
        }
        return BookingResponseDTO.desde(reserva);
    }

    private DatosReserva resolverDatos(BookingRequestDTO solicitud) {
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

        return new DatosReserva(publicacionId, fechaLlegada, fechaSalida, cantidadHuespedes, montoTotal);
    }

    private void notificar(Long usuarioId, Booking reserva, String mensaje) {
        serviciosExternos.notificarReservaConfirmada(
                usuarioId, reserva.getId(), reserva.getCodigoReserva(), mensaje);
    }

    private static void validarDatos(DatosReserva datos) {
        if (datos.publicacionId() == null) {
            throw new SolicitudInvalidaException("La publicación es obligatoria");
        }
        if (datos.fechaLlegada() == null || datos.fechaSalida() == null) {
            throw new FechasInvalidasException("Las fechas de llegada y salida son obligatorias");
        }
        if (!datos.fechaLlegada().isBefore(datos.fechaSalida())) {
            throw new FechasInvalidasException("La fecha de llegada debe ser anterior a la de salida");
        }
        if (datos.cantidadHuespedes() == null || datos.cantidadHuespedes() <= 0) {
            throw new SolicitudInvalidaException("La cantidad de huéspedes debe ser mayor a 0");
        }
        if (datos.montoTotal() == null || datos.montoTotal().signum() < 0) {
            throw new SolicitudInvalidaException("El monto total es obligatorio y no puede ser negativo");
        }
    }

    private static String generarCodigoReserva() {
        return "RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private record DatosReserva(Long publicacionId, LocalDate fechaLlegada, LocalDate fechaSalida,
            Integer cantidadHuespedes, BigDecimal montoTotal) {
    }
}
