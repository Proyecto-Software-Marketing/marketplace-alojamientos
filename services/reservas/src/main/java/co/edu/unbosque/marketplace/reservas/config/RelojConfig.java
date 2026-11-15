package co.edu.unbosque.marketplace.reservas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// HU-27: reloj del servicio. El cierre de estancias lo usa en vez de LocalDate.now()
// directo para que las pruebas puedan fijar el día de hoy
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${reservas.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
