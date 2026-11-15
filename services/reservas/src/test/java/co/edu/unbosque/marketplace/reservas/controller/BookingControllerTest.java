package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.client.CotizacionDTO;
import co.edu.unbosque.marketplace.reservas.client.ServiciosExternos;
import co.edu.unbosque.marketplace.reservas.model.Booking;
import co.edu.unbosque.marketplace.reservas.model.BookingStatus;
import co.edu.unbosque.marketplace.reservas.repository.BookingRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookingControllerTest {

    private static final String TOKEN = "Bearer token-huesped-valido";
    private static final Long HUESPED_ID = 25L;
    private static final Long PUBLICACION_ID = 101L;
    private static final String COTIZACION_ID = "cot-abc-123";
    private static final LocalDate LLEGADA = LocalDate.now().plusDays(5);
    private static final LocalDate SALIDA = LocalDate.now().plusDays(10);
    private static final BigDecimal TOTAL = new BigDecimal("450000.00");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private ServiciosExternos serviciosExternos;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        bookingRepository.deleteAll();
    }

    @Test
    @DisplayName("CA-064 POST /bookings crea la reserva confirmada directamente cuando está disponible")
    void crearReservaDisponibleExitoso() throws Exception {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.consultarCotizacion(COTIZACION_ID))
                .thenReturn(new CotizacionDTO(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA, 2, TOTAL));
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(true);

        String json = """
                {
                    "cotizacionId": "%s",
                    "metodoPago": "pm_card_visa",
                    "publicacionId": %d,
                    "fechaLlegada": "%s",
                    "fechaSalida": "%s",
                    "cantidadHuespedes": 2,
                    "montoTotal": 450000.00
                }
                """.formatted(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", TOKEN)
                        .header("Idempotency-Key", "idemp-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.codigoReserva", startsWithReserva()))
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")))
                .andExpect(jsonPath("$.huespedId", is(HUESPED_ID.intValue())))
                .andExpect(jsonPath("$.publicacionId", is(PUBLICACION_ID.intValue())));
    }

    @Test
    @DisplayName("CA-065 POST /bookings rechaza con 409 cuando ya no está disponible")
    void crearReservaNoDisponibleRechaza() throws Exception {
        when(serviciosExternos.verificarRolHuesped(TOKEN)).thenReturn(HUESPED_ID);
        when(serviciosExternos.consultarCotizacion(COTIZACION_ID))
                .thenReturn(new CotizacionDTO(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA, 2, TOTAL));
        when(serviciosExternos.verificarDisponibilidad(PUBLICACION_ID, LLEGADA, SALIDA)).thenReturn(false);

        String json = """
                {
                    "cotizacionId": "%s",
                    "metodoPago": "pm_card_visa",
                    "publicacionId": %d,
                    "fechaLlegada": "%s",
                    "fechaSalida": "%s"
                }
                """.formatted(COTIZACION_ID, PUBLICACION_ID, LLEGADA, SALIDA);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes", hasItem(containsString("El alojamiento ya no está disponible"))));
    }

    @Test
    @DisplayName("POST /bookings sin token de sesión responde 401")
    void crearReservaSinSesionResponde401() throws Exception {
        String json = """
                {
                    "cotizacionId": "cot-1",
                    "metodoPago": "pm_card_visa"
                }
                """;

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes", hasItem("Debe iniciar sesión")));
    }

    @Test
    @DisplayName("POST /bookings con datos faltantes responde 400")
    void crearReservaDatosFaltantesResponde400() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/bookings")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", notNullValue()));
    }

    @Test
    @DisplayName("GET /bookings/{id} consulta la reserva correctamente")
    void consultarReservaPorId() throws Exception {
        Booking booking = new Booking(
                "RES-12345678",
                PUBLICACION_ID,
                HUESPED_ID,
                COTIZACION_ID,
                LLEGADA,
                SALIDA,
                2,
                TOTAL,
                "pm_card_visa",
                "idemp-xyz",
                BookingStatus.CONFIRMADA,
                LocalDateTime.now()
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(get("/bookings/" + booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.codigoReserva", is("RES-12345678")))
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));
    }

    private static org.hamcrest.Matcher<String> startsWithReserva() {
        return org.hamcrest.Matchers.startsWith("RES-");
    }
}
