package co.edu.unbosque.marketplace.reservas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// HU-27: habilita las tareas programadas del servicio (cierre diario de estancias)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
