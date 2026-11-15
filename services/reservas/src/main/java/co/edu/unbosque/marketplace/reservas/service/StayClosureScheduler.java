package co.edu.unbosque.marketplace.reservas.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// HU-27: ejecuta el cierre de estancias todos los días (por defecto a las 00:05 en Bogotá).
// El horario se cambia con la propiedad reservas.cierre-estancias.cron.
@Component
public class StayClosureScheduler {

    private final StayClosureService stayClosureService;

    public StayClosureScheduler(StayClosureService stayClosureService) {
        this.stayClosureService = stayClosureService;
    }

    @Scheduled(cron = "${reservas.cierre-estancias.cron:0 5 0 * * *}",
            zone = "${reservas.zona-horaria:America/Bogota}")
    public void ejecutarCierreDiario() {
        stayClosureService.cerrarEstanciasTerminadas();
    }
}
