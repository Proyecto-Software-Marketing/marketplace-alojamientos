package co.edu.unbosque.marketplace.reservas.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

// Pruebas de la tarea programada de la HU-27 Cierre automático de estancias
@SpringBootTest
class StayClosureSchedulerTest {

    @Autowired
    private StayClosureScheduler stayClosureScheduler;

    @Autowired
    private Environment entorno;

    @MockitoBean
    private StayClosureService stayClosureService;

    @Test
    @DisplayName("Decisión 2: la tarea diaria ejecuta el mismo cierre de estancias que la ruta interna")
    void tareaDiariaEjecutaCierre() {
        // Cuando se dispara la tarea programada
        stayClosureScheduler.ejecutarCierreDiario();

        // Entonces se ejecuta el cierre de estancias
        verify(stayClosureService).cerrarEstanciasTerminadas();
    }

    @Test
    @DisplayName("Decisión 2: la tarea está programada todos los días a las 00:05 en America/Bogota")
    void horarioDiarioEnBogota() throws NoSuchMethodException {
        // Dado el horario configurado y la anotación de la tarea
        Scheduled programacion = StayClosureScheduler.class.getMethod("ejecutarCierreDiario")
                .getAnnotation(Scheduled.class);
        assertNotNull(programacion);
        String cron = entorno.getProperty("reservas.cierre-estancias.cron");
        ZoneId zona = ZoneId.of(entorno.getProperty("reservas.zona-horaria"));

        // Cuando se calcula la siguiente ejecución a partir del 15 de noviembre a las 16:00
        ZonedDateTime siguiente = CronExpression.parse(cron)
                .next(ZonedDateTime.of(2026, 11, 15, 16, 0, 0, 0, zona));

        // Entonces es el 16 de noviembre a las 00:05 en Bogotá
        assertEquals(ZonedDateTime.of(2026, 11, 16, 0, 5, 0, 0, ZoneId.of("America/Bogota")), siguiente);
    }
}
