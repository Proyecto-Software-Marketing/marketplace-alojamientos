package co.edu.unbosque.marketplace.cotizacion.controller;

import co.edu.unbosque.marketplace.cotizacion.dto.TarifaRequest;
import co.edu.unbosque.marketplace.cotizacion.dto.TarifaResponse;
import co.edu.unbosque.marketplace.cotizacion.service.TarifaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/tarifas")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    // Consultar el porcentaje actual
    @GetMapping("/servicio")
    public ResponseEntity<TarifaResponse> consultarTarifa() {
        return ResponseEntity.ok(new TarifaResponse(tarifaService.consultarPorcentaje()));
    }

    //cambio de porcentaje
    @PutMapping("/servicio")
    public ResponseEntity<TarifaResponse> cambiarTarifa(@RequestHeader("Authorization") String autorizacion,
            @Valid @RequestBody TarifaRequest datos) {
        return ResponseEntity.ok(tarifaService.cambiarPorcentaje(autorizacion, datos));
    }
}