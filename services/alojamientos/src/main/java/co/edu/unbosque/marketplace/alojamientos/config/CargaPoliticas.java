package co.edu.unbosque.marketplace.alojamientos.config;

import co.edu.unbosque.marketplace.alojamientos.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.alojamientos.repository.PoliticaCancelacionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class CargaPoliticas implements CommandLineRunner {

    private final PoliticaCancelacionRepository politicaRepository;

    public CargaPoliticas(PoliticaCancelacionRepository politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    @Override
    public void run(String... args) {
        // si ya existen no se vuelven a crear
        if (politicaRepository.count() > 0) {
            return;
        }
        politicaRepository.save(new PoliticaCancelacion(
                "Flexible: devolución del 100 %",
                "Si el huésped cancela, se le devuelve todo y las fechas quedan libres otra vez.",
                new BigDecimal("100"), true));
        politicaRepository.save(new PoliticaCancelacion(
                "Moderada: devolución del 50 %",
                "Si el huésped cancela, se le devuelve la mitad y las fechas quedan libres otra vez.",
                new BigDecimal("50"), true));
        politicaRepository.save(new PoliticaCancelacion(
                "Estricta: sin devolución",
                "Si el huésped cancela, no se le devuelve dinero y las fechas siguen bloqueadas.",
                BigDecimal.ZERO, false));
    }
}