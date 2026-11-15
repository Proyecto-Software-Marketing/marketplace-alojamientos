package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.dto.BookingListDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.BookingResponseDTO;
import co.edu.unbosque.marketplace.reservas.exception.ConflictoReservaException;
import co.edu.unbosque.marketplace.reservas.exception.NoEncontradoException;
import co.edu.unbosque.marketplace.reservas.exception.PermisoException;
import co.edu.unbosque.marketplace.reservas.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de las rutas públicas de reservas: HU-23 (POST), HU-24 (GET) y HU-26 (GET por id)
@SpringBootTest
class BookingControllerTest {

    private static final String RUTA = "/bookings";
    private static final String TOKEN = "Bearer token-huesped";
    private static final String CLAVE = "idemp-123";
    private static final LocalDate LLEGADA = LocalDate.now().plusDays(5);
    private static final LocalDate SALIDA = LocalDate.now().plusDays(8);

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private BookingService bookingService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private String cuerpo() {
        return """
                {"cotizacionId": "cot-1", "metodoPago": "pm_card_visa",
                 "publicacionId": 101, "fechaLlegada": "%s", "fechaSalida": "%s",
                 "cantidadHuespedes": 2, "montoTotal": 450000.00}
                """.formatted(LLEGADA, SALIDA);
    }

    private BookingResponseDTO respuesta() {
        return new BookingResponseDTO(1L, "RES-ABCD1234", 101L, 25L, LLEGADA, SALIDA, 2,
                new BigDecimal("450000.00"), BookingStatus.CONFIRMADA,
                LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    @DisplayName("POST /bookings responde 201 con la reserva confirmada")
    void responde201() throws Exception {
        when(bookingService.crearReserva(eq(TOKEN), eq(CLAVE), any(BookingRequestDTO.class)))
                .thenReturn(respuesta());

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigoReserva").value("RES-ABCD1234"))
                .andExpect(jsonPath("$.publicacionId").value(101))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("Sin Authorization responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("Sin Idempotency-Key responde 400")
    void sinClaveResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Falta el encabezado obligatorio: Idempotency-Key"));
    }

    @Test
    @DisplayName("Si el usuario no es huésped responde 403")
    void sinRolHuespedResponde403() throws Exception {
        when(bookingService.crearReserva(anyString(), anyString(), any(BookingRequestDTO.class)))
                .thenThrow(new PermisoException("Solo un huésped puede realizar una reserva"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo un huésped puede realizar una reserva"));
    }

    @Test
    @DisplayName("CA-065 si el periodo ya no está disponible responde 409")
    void periodoNoDisponibleResponde409() throws Exception {
        when(bookingService.crearReserva(anyString(), anyString(), any(BookingRequestDTO.class)))
                .thenThrow(new ConflictoReservaException(
                        "El alojamiento ya no está disponible para las fechas seleccionadas"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El alojamiento ya no está disponible para las fechas seleccionadas"));
    }

    @Test
    @DisplayName("Si un servicio externo no responde, responde 503")
    void servicioCaidoResponde503() throws Exception {
        when(bookingService.crearReserva(anyString(), anyString(), any(BookingRequestDTO.class)))
                .thenThrow(new ServicioNoDisponibleException(
                        "No fue posible verificar la disponibilidad. Intente más tarde"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Sin cotización ni método de pago responde 400")
    void validacionResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(2));
    }

    // ---------- HU-24: GET /bookings ----------

    @Test
    @DisplayName("CA-068 GET /bookings responde 200 con las reservas del huésped")
    void listarReservasResponde200() throws Exception {
        when(bookingService.listarReservas(eq(TOKEN), any()))
                .thenReturn(new BookingListDTO(List.of(respuesta())));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].codigoReserva").value("RES-ABCD1234"))
                .andExpect(jsonPath("$.items[0].estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("GET /bookings filtra por estado")
    void listarReservasFiltraPorEstado() throws Exception {
        when(bookingService.listarReservas(eq(TOKEN), eq(BookingStatus.CONFIRMADA)))
                .thenReturn(new BookingListDTO(List.of(respuesta())));

        mockMvc.perform(get(RUTA)
                        .header("Authorization", TOKEN)
                        .param("estado", "CONFIRMADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("GET /bookings sin Authorization responde 401")
    void listarReservasSinTokenResponde401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("GET /bookings si el usuario no es huésped responde 403")
    void listarReservasSinRolHuespedResponde403() throws Exception {
        when(bookingService.listarReservas(anyString(), any()))
                .thenThrow(new PermisoException("Solo un huésped puede realizar una reserva"));

        mockMvc.perform(get(RUTA).header("Authorization", TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bookings con un estado inválido responde 400")
    void listarReservasConEstadoInvalidoResponde400() throws Exception {
        mockMvc.perform(get(RUTA)
                        .header("Authorization", TOKEN)
                        .param("estado", "NO_EXISTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }

    // ---------- HU-26: GET /bookings/{id} ----------

    @Test
    @DisplayName("HU-26 GET /bookings/{id} responde 200 con el estado de la reserva")
    void consultarReservaResponde200() throws Exception {
        when(bookingService.consultarReserva(TOKEN, 1L)).thenReturn(respuesta());

        mockMvc.perform(get(RUTA + "/1").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigoReserva").value("RES-ABCD1234"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("HU-26 GET /bookings/{id} sin Authorization responde 401")
    void consultarReservaSinTokenResponde401() throws Exception {
        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("HU-26 GET /bookings/{id} de una reserva ajena responde 403")
    void consultarReservaAjenaResponde403() throws Exception {
        when(bookingService.consultarReserva(anyString(), eq(1L)))
                .thenThrow(new PermisoException("No tiene permiso para consultar esta reserva"));

        mockMvc.perform(get(RUTA + "/1").header("Authorization", TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("No tiene permiso para consultar esta reserva"));
    }

    @Test
    @DisplayName("HU-26 GET /bookings/{id} inexistente responde 404")
    void consultarReservaInexistenteResponde404() throws Exception {
        when(bookingService.consultarReserva(anyString(), eq(999L)))
                .thenThrow(new NoEncontradoException("La reserva no existe"));

        mockMvc.perform(get(RUTA + "/999").header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La reserva no existe"));
    }

    @Test
    @DisplayName("HU-26 GET /bookings/{id} con un id no numérico responde 400")
    void consultarReservaConIdInvalidoResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/abc").header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }
}
