package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.dto.StayClosureResultDTO;
import co.edu.unbosque.marketplace.reservas.service.StayClosureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de la ruta interna de la HU-27 Cierre automático de estancias
@SpringBootTest
class InternalBookingControllerTest {

    private static final String RUTA = "/internal/bookings/close-stays";

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private StayClosureService stayClosureService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    @DisplayName("CA-075 POST /internal/bookings/close-stays ejecuta el cierre y responde 200 con la cantidad completada")
    void ejecutaCierreYRespondeCantidad() throws Exception {
        // Dado que hay 3 estancias terminadas por cerrar
        when(stayClosureService.cerrarEstanciasTerminadas()).thenReturn(new StayClosureResultDTO(3));

        // Cuando otro servicio llama al cierre, Entonces responde 200 con reservasCompletadas
        mockMvc.perform(post(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasCompletadas").value(3));
        verify(stayClosureService).cerrarEstanciasTerminadas();
    }

    @Test
    @DisplayName("Decisión 4: la ruta interna no pide token y responde 200 aunque no haya nada por cerrar")
    void sinTokenYSinReservasResponde200() throws Exception {
        // Dado que no hay estancias por cerrar
        when(stayClosureService.cerrarEstanciasTerminadas()).thenReturn(new StayClosureResultDTO(0));

        // Cuando se llama sin encabezado Authorization, Entonces responde 200 con 0
        mockMvc.perform(post(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasCompletadas").value(0));
    }
}
