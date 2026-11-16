package co.edu.unbosque.marketplace.cotizacion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// Reloj para saber qué día es hoy (HU-22, decisión 4). Las pruebas lo reemplazan para fijar la fecha
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${cotizacion.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
