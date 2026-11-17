package co.edu.unbosque.marketplace.reservas.controller;

import co.edu.unbosque.marketplace.reservas.service.RevisionManualReservasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RevisionManualReservasController.class)
class RevisionManualReservasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevisionManualReservasService servicio;

    @Test
    void solicitudValidaActualizaReservas() throws Exception {
        when(servicio.revisarReservas(15L)).thenReturn(3);

        mockMvc.perform(post("/internal/bookings/review-manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicacionId\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicacionId").value(15))
                .andExpect(jsonPath("$.reservasActualizadas").value(3));

        verify(servicio).revisarReservas(15L);
    }

    @Test
    void identificadorNegativoEsRechazado() throws Exception {
        mockMvc.perform(post("/internal/bookings/review-manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicacionId\":-1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void identificadorFaltanteEsRechazado() throws Exception {
        mockMvc.perform(post("/internal/bookings/review-manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }
}