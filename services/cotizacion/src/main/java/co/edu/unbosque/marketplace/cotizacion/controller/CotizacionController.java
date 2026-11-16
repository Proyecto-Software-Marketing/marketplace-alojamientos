package co.edu.unbosque.marketplace.cotizacion.controller;

import co.edu.unbosque.marketplace.cotizacion.client.ClienteCotizacion;
import co.edu.unbosque.marketplace.cotizacion.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.cotizacion.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.cotizacion.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU-22: cotización desglosada
@RestController
@RequestMapping("/quotes")
public class CotizacionController {

    private final CotizacionService cotizacionService;
    private final ClienteCotizacion clienteCotizacion;

    public CotizacionController(CotizacionService cotizacionService, ClienteCotizacion clienteCotizacion) {
        this.cotizacionService = cotizacionService;
        this.clienteCotizacion = clienteCotizacion;
    }

    // CA-060 a CA-063. Rol requerido: HUESPED
    @PostMapping
    public ResponseEntity<CotizacionResponse> cotizar(@RequestHeader("Authorization") String autorizacion,
            @Valid @RequestBody CotizacionRequest datos) {
        Long idHuesped = clienteCotizacion.verificarHuesped(autorizacion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CotizacionResponse(cotizacionService.cotizar(idHuesped, datos)));
    }

    // Decisión 1: la consulta Reservas antes de confirmar la reserva (sin token)
    @GetMapping("/{id}")
    public ResponseEntity<CotizacionResponse> consultar(@PathVariable("id") String id) {
        return ResponseEntity.ok(new CotizacionResponse(cotizacionService.consultar(id)));
    }
}
