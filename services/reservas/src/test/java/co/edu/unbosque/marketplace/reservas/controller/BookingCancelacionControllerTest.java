package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.dto.CancelRequestDTO;
import co.edu.unbosque.marketplace.reservas.dto.CancellationResponseDTO;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de POST /bookings/{id}/cancel (HU-28)
@SpringBootTest
class BookingCancelacionControllerTest {

    private static final String RUTA = "/bookings/1/cancel";
    private static final String TOKEN = "Bearer token-huesped";

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private BookingService bookingService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private CancellationResponseDTO respuesta() {
        return new CancellationResponseDTO(
                1L,
                25L,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "Viaje cancelado",
                new BigDecimal("225000.00"),
                BookingStatus.CANCELADA);
    }

    @Test
    @DisplayName("CA-077 POST /bookings/{id}/cancel responde 200 con la devolución simulada")
    void cancelarReservaResponde200() throws Exception {
        when(bookingService.cancelarReserva(eq(TOKEN), eq(1L), any(CancelRequestDTO.class)))
                .thenReturn(respuesta());

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(1))
                .andExpect(jsonPath("$.canceladaPor").value(25))
                .andExpect(jsonPath("$.motivo").value("Viaje cancelado"))
                .andExpect(jsonPath("$.valorDevolucionSimulada").value(225000.00))
                .andExpect(jsonPath("$.estadoFinal").value("CANCELADA"));
    }

    @Test
    @DisplayName("Sin Authorization responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Debe iniciar sesión"));
    }

    @Test
    @DisplayName("Sin motivo responde 400")
    void sinMotivoResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El motivo de cancelación es obligatorio"));
    }

    @Test
    @DisplayName("Un motivo demasiado corto responde 400")
    void motivoCortoResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"ok\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El motivo debe tener entre 3 y 500 caracteres"));
    }

    @Test
    @DisplayName("El motivo se valida después de quitar espacios")
    void motivoConEspaciosCortoResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  a  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El motivo debe tener entre 3 y 500 caracteres"));
    }

    @Test
    @DisplayName("CA-079 si el usuario es anfitrión responde 403")
    void anfitrionResponde403() throws Exception {
        when(bookingService.cancelarReserva(anyString(), eq(1L), any(CancelRequestDTO.class)))
                .thenThrow(new PermisoException("No tiene permiso para cancelar esta reserva"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value("No tiene permiso para cancelar esta reserva"));
    }

    @Test
    @DisplayName("Una reserva inexistente responde 404")
    void reservaInexistenteResponde404() throws Exception {
        when(bookingService.cancelarReserva(anyString(), eq(1L), any(CancelRequestDTO.class)))
                .thenThrow(new NoEncontradoException("La reserva no existe"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("La reserva no existe"));
    }

    @Test
    @DisplayName("Una reserva que no está CONFIRMADA responde 409")
    void estadoNoPermitidoResponde409() throws Exception {
        when(bookingService.cancelarReserva(anyString(), eq(1L), any(CancelRequestDTO.class)))
                .thenThrow(new ConflictoReservaException("Solo se puede cancelar una reserva CONFIRMADA"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("Solo se puede cancelar una reserva CONFIRMADA"));
    }

    @Test
    @DisplayName("Si Alojamientos no responde responde 503 y no cancela")
    void politicaNoDisponibleResponde503() throws Exception {
        when(bookingService.cancelarReserva(anyString(), eq(1L), any(CancelRequestDTO.class)))
                .thenThrow(new ServicioNoDisponibleException(
                        "No fue posible consultar la política de cancelación. Intente más tarde"));

        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("No fue posible consultar la política de cancelación. Intente más tarde"));
    }

    @Test
    @DisplayName("Un id no numérico responde 400")
    void idInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/bookings/abc/cancel")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Viaje cancelado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El identificador enviado no es válido"));
    }

    @Test
    @DisplayName("Un JSON mal formado responde 400")
    void jsonInvalidoResponde400() throws Exception {
        mockMvc.perform(post(RUTA)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Los datos enviados no tienen el formato correcto"));
    }
}
