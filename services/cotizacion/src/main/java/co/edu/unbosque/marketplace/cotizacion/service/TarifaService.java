package co.edu.unbosque.marketplace.cotizacion.service;

import co.edu.unbosque.marketplace.cotizacion.client.ServiciosExternos;
import co.edu.unbosque.marketplace.cotizacion.dto.TarifaRequest;
import co.edu.unbosque.marketplace.cotizacion.dto.TarifaResponse;
import co.edu.unbosque.marketplace.cotizacion.model.ConfiguracionTarifa;
import co.edu.unbosque.marketplace.cotizacion.repository.ConfiguracionTarifaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// Manejo del porcentaje de la tarifa de servicio
@Service
public class TarifaService {

    private final ConfiguracionTarifaRepository configuracionTarifaRepository;
    private final ServiciosExternos serviciosExternos;

    public TarifaService(ConfiguracionTarifaRepository configuracionTarifaRepository,
            ServiciosExternos serviciosExternos) {
        this.configuracionTarifaRepository = configuracionTarifaRepository;
        this.serviciosExternos = serviciosExternos;
    }

    // Metodo paar que las cotizaciones sepan que porcentaje cobrar
    public BigDecimal consultarPorcentaje() {
        return configuracionTarifaRepository.findTopByOrderByIdDesc()
                .map(configuracion -> configuracion.getPorcentaje())
                .orElse(BigDecimal.ZERO);
    }

    // Cambio de porcentaje por el admin
    public TarifaResponse cambiarPorcentaje(String autorizacion, TarifaRequest datos) {

        Long idAdministrador = serviciosExternos.verificarAdministrador(autorizacion);

        BigDecimal anterior = consultarPorcentaje();
        configuracionTarifaRepository.save(new ConfiguracionTarifa(datos.getPorcentaje(), idAdministrador));

        serviciosExternos.registrarCambioTarifa(idAdministrador, anterior, datos.getPorcentaje());

        return new TarifaResponse(datos.getPorcentaje());
    }
}