package co.edu.unbosque.marketplace.identidad.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// Reloj del servicio, en las pruebas se reemplaza para fijar la hora (HU-04 vencimiento del token)
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${identidad.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
