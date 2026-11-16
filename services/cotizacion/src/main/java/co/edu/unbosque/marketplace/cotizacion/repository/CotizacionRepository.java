package co.edu.unbosque.marketplace.cotizacion.repository;

import co.edu.unbosque.marketplace.cotizacion.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

// HU-22: cotizaciones guardadas, por su id en texto (UUID)
public interface CotizacionRepository extends JpaRepository<Cotizacion, String> {
}
