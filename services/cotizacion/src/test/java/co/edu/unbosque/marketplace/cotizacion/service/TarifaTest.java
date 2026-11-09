package co.edu.unbosque.marketplace.cotizacion.service;

import co.edu.unbosque.marketplace.cotizacion.client.ServiciosExternos;
import co.edu.unbosque.marketplace.cotizacion.dto.TarifaRequest;
import co.edu.unbosque.marketplace.cotizacion.exception.PermisoException;
import co.edu.unbosque.marketplace.cotizacion.repository.ConfiguracionTarifaRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la HU-38 para configurar tarifa de servicio 
@SpringBootTest
class TarifaTest {

    private static final String TOKEN_ADMIN = "Bearer token-admin";
    private static final String TOKEN_HUESPED = "Bearer token-huesped";

    @Autowired
    private TarifaService tarifaService;

    @Autowired
    private ConfiguracionTarifaRepository configuracionTarifaRepository;

    @Autowired
    private Validator validador;

    // Simula Identidad y Auditoría
    @MockitoBean
    private ServiciosExternos serviciosExternos;

    //simula quién es admin y quien no
    @BeforeEach
    void prepararDatos() {
        configuracionTarifaRepository.deleteAll();
        when(serviciosExternos.verificarAdministrador(TOKEN_ADMIN)).thenReturn(1L);
        when(serviciosExternos.verificarAdministrador(TOKEN_HUESPED))
                .thenThrow(new PermisoException("Solo el administrador puede cambiar la tarifa de servicio"));
    }

    @Test
    @DisplayName("Si el admin no la ha configurado la tarifa es 0")
    void tarifaInicialEsCero() {
        assertEquals(0, tarifaService.consultarPorcentaje().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("CA-105 el admin cambia el porcentaje y las cotizaciones lo usan")
    void adminCambiaElPorcentaje() {
        tarifaService.cambiarPorcentaje(TOKEN_ADMIN, new TarifaRequest(new BigDecimal("15")));

        assertEquals(0, tarifaService.consultarPorcentaje().compareTo(new BigDecimal("15")));
    }

    @Test
    @DisplayName("CA-105 se usa el último porcentaje que puso el admin")
    void seUsaElUltimoCambio() {
        tarifaService.cambiarPorcentaje(TOKEN_ADMIN, new TarifaRequest(new BigDecimal("15")));
        tarifaService.cambiarPorcentaje(TOKEN_ADMIN, new TarifaRequest(new BigDecimal("12.5")));

        assertEquals(0, tarifaService.consultarPorcentaje().compareTo(new BigDecimal("12.5")));
    }

    @Test
    @DisplayName("CA-106 un porcentaje negativo no pasa")
    void negativoNoPasa() {
        TarifaRequest datos = new TarifaRequest(new BigDecimal("-5"));

        assertFalse(validador.validate(datos).isEmpty());
    }

    @Test
    @DisplayName("CA-106 un porcentaje mayor a 100 no pasa")
    void mayorACienNoPasa() {
        TarifaRequest datos = new TarifaRequest(new BigDecimal("150"));

        assertFalse(validador.validate(datos).isEmpty());
    }

    @Test
    @DisplayName("CA-106 un porcentaje vacío no pasa")
    void vacioNoPasa() {
        TarifaRequest datos = new TarifaRequest(null);

        assertFalse(validador.validate(datos).isEmpty());
    }

    @Test
    @DisplayName("Si no es admin no puede cambiar la tarifa")
    void noAdminNoPuedeCambiar() {
        TarifaRequest datos = new TarifaRequest(new BigDecimal("20"));

        assertThrows(PermisoException.class, () -> tarifaService.cambiarPorcentaje(TOKEN_HUESPED, datos));
        assertEquals(0, tarifaService.consultarPorcentaje().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("CA-107 el cambio se avisa a Auditoría")
    void cambioQuedaEnAuditoria() {
        BigDecimal nuevo = new BigDecimal("15");

        tarifaService.cambiarPorcentaje(TOKEN_ADMIN, new TarifaRequest(nuevo));

        verify(serviciosExternos).registrarCambioTarifa(1L, BigDecimal.ZERO, nuevo);
    }
}