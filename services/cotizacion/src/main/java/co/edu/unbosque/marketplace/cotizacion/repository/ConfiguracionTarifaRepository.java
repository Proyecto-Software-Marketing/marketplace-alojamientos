package co.edu.unbosque.marketplace.cotizacion.repository;

import co.edu.unbosque.marketplace.cotizacion.model.ConfiguracionTarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionTarifaRepository extends JpaRepository<ConfiguracionTarifa, Long> {

    // Trae el ultimo cambio, que es el porcentaje actual
    Optional<ConfiguracionTarifa> findTopByOrderByIdDesc();
}