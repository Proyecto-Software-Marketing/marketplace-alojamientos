package co.edu.unbosque.marketplace.notificaciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// Reloj del servicio. Se inyecta en vez de usar LocalDateTime.now() directo
// para que las pruebas puedan fijar la hora y comprobar la fecha (CA-084)
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${notificaciones.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
