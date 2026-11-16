package co.edu.unbosque.marketplace.alojamientos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// Reloj del servicio. Se inyecta en vez de usar LocalDateTime.now() directo
// para que las pruebas puedan fijar la hora y comprobar la fecha de creación de la publicación
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${alojamientos.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
