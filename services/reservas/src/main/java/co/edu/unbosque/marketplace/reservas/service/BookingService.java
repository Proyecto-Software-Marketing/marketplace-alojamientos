package co.edu.unbosque.marketplace.reservas.service;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.PoliticaCancelacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.client.UsuarioIdentidadDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingListDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.dto.CancelRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.CancellationResponseDTO;
import co.edu.unbosque.marketplace.reservas.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.FechasInvalidasException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.reservas.exception.SolicitudInvalidaException;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// HU-23: reserva inmediata sin aprobación del anfitrión.
// HU-24: un reintento (incluso simultáneo) nunca crea dos reservas ni dos pagos.
// HU-26: historial y estado de reservas para huésped y anfitrión.
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

    // CA-068 (HU-24) y HU-26: el huésped ve sus reservas y el anfitrión las de sus publicaciones
    public BookingListDTO listarReservas(String autorizacion, BookingStatus estado) {
        UsuarioIdentidadDTO usuario = serviciosExternos.verificarSesion(autorizacion);

        List<Booking> reservas;
        if ("ANFITRION".equalsIgnoreCase(usuario.rol())) {
            reservas = estado == null
                    ? bookingRepository.findByAnfitrionIdOrderByCreadaEnDesc(usuario.id())
                    : bookingRepository.findByAnfitrionIdAndEstadoOrderByCreadaEnDesc(usuario.id(), estado);
        } else {
            reservas = estado == null
                    ? bookingRepository.findByHuespedIdOrderByCreadaEnDesc(usuario.id())
                    : bookingRepository.findByHuespedIdAndEstadoOrderByCreadaEnDesc(usuario.id(), estado);
        }

        return new BookingListDTO(reservas.stream().map(BookingResponseDTO::desde).toList());
    }

    // HU-26: detalle de una reserva; solo su huésped o el anfitrión de la publicación
    public BookingResponseDTO consultarReserva(String autorizacion, Long reservaId) {
        UsuarioIdentidadDTO usuario = serviciosExternos.verificarSesion(autorizacion);

        Booking reserva = bookingRepository.findById(reservaId)
                .orElseThrow(() -> new NoEncontradoException("La reserva no existe"));

        boolean esHuesped = reserva.getHuespedId().equals(usuario.id());
        boolean esAnfitrion = reserva.getAnfitrionId() != null
                && reserva.getAnfitrionId().equals(usuario.id());
        if (!esHuesped && !esAnfitrion) {
            throw new PermisoException("No tiene permiso para consultar esta reserva");
        }

        return BookingResponseDTO.desde(reserva);
    }

    // HU-28: el huésped cancela su reserva o el administrador cancela cualquiera.
    // CA-077/CA-078: devolución simulada según la política de la publicación.
    // CA-079: el anfitrión no puede cancelar (Identidad solo autoriza HUESPED/ADMINISTRADOR).
    // CA-080: si la política lo permite, se libera el periodo reservado.
    // CA-128: se notifica al huésped y al anfitrión.
    public CancellationResponseDTO cancelarReserva(String autorizacion, Long reservaId, CancelRequestDTO solicitud) {
        UsuarioIdentidadDTO usuario = serviciosExternos.verificarSesionCancelacion(autorizacion);
        CancellationResponseDTO cancelacion = transaccion
                .execute(estado -> cancelarReservaEnTransaccion(usuario, reservaId, solicitud));
        registrarAuditoriaCancelacion(cancelacion);
        return cancelacion;
    }

    private void registrarAuditoriaCancelacion(CancellationResponseDTO cancelacion) {
        String detalle = "Fecha de cancelación: " + cancelacion.fecha()
                + "; Motivo: " + cancelacion.motivo()
                + "; Devolución simulada: " + cancelacion.valorDevolucionSimulada().toPlainString()
                + "; Estado final: " + cancelacion.estadoFinal();

        serviciosExternos.registrarAuditoria(new EventoAuditoriaDTO(
                cancelacion.canceladaPor(),
                "RESERVA",
                cancelacion.reservaId(),
                "CANCELAR_RESERVA",
                "EXITO",
                detalle));
    }

    private CancellationResponseDTO cancelarReservaEnTransaccion(UsuarioIdentidadDTO usuario, Long reservaId,
            CancelRequestDTO solicitud) {
        Booking reserva = bookingRepository.findById(reservaId)
                .orElseThrow(() -> new NoEncontradoException("La reserva no existe"));

        boolean esAdministrador = "ADMINISTRADOR".equalsIgnoreCase(usuario.rol());
        if (!esAdministrador && !reserva.getHuespedId().equals(usuario.id())) {
            throw new PermisoException("No tiene permiso para cancelar esta reserva");
        }
        if (reserva.getEstado() != BookingStatus.CONFIRMADA) {
            throw new ConflictoReservaException("Solo se puede cancelar una reserva CONFIRMADA");
        }

        PoliticaCancelacionDTO politica = serviciosExternos
                .consultarPoliticaCancelacion(reserva.getPublicacionId());
        BigDecimal devolucion = calcularDevolucion(reserva.getMontoTotal(), politica);

        reserva.cancelar(usuario.id(), solicitud.motivo(), LocalDateTime.now(), devolucion);
        Booking cancelada = bookingRepository.saveAndFlush(reserva);

        // Si no se puede liberar el periodo, la transacción revierte la cancelación.
        if (Boolean.TRUE.equals(politica.permiteLiberacionPeriodo())) {
            serviciosExternos.liberarPeriodo(cancelada.getPublicacionId(), cancelada.getId(),
                    cancelada.getFechaLlegada(), cancelada.getFechaSalida());
        }

        notificarCancelacion(cancelada);

        return new CancellationResponseDTO(
                cancelada.getId(),
                cancelada.getCanceladaPor(),
                cancelada.getFechaCancelacion(),
                cancelada.getMotivoCancelacion(),
                cancelada.getValorDevolucionSimulada(),
                cancelada.getEstado());
    }

    private static BigDecimal calcularDevolucion(BigDecimal montoTotal, PoliticaCancelacionDTO politica) {
        if (politica == null || politica.porcentajeDevolucion() == null
                || politica.porcentajeDevolucion().compareTo(BigDecimal.ZERO) < 0
                || politica.porcentajeDevolucion().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ServicioNoDisponibleException(
                    "La política de cancelación de la publicación no es válida");
        }
        return montoTotal.multiply(politica.porcentajeDevolucion())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void notificarCancelacion(Booking reserva) {
        serviciosExternos.notificarReservaCancelada(
                reserva.getHuespedId(),
                reserva.getId(),
                reserva.getCodigoReserva(),
                "Tu reserva " + reserva.getCodigoReserva() + " fue cancelada. Devolución simulada: "
                        + reserva.getValorDevolucionSimulada().toPlainString());

        Long anfitrionId = reserva.getAnfitrionId();
        if (anfitrionId == null) {
            anfitrionId = serviciosExternos.consultarAnfitrion(reserva.getPublicacionId());
        }
        if (anfitrionId != null && !anfitrionId.equals(reserva.getHuespedId())) {
            serviciosExternos.notificarReservaCancelada(
                    anfitrionId,
                    reserva.getId(),
                    reserva.getCodigoReserva(),
                    "La reserva " + reserva.getCodigoReserva() + " de tu publicación fue cancelada");
        }
    }

    // CA-067: una única ejecución confirma la reserva y sus efectos externos
    private BookingResponseDTO confirmarReserva(Long huespedId, String idempotencyKey,
            BookingRequestDTO solicitud, DatosReserva datos) {
        // HU-26: se guarda el anfitrión para que pueda consultar el historial
        Long anfitrionId = serviciosExternos.consultarAnfitrion(datos.publicacionId());

        Booking reserva = new Booking(
                generarCodigoReserva(),
                datos.publicacionId(),
                huespedId,
                anfitrionId,
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
