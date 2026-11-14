package co.edu.unbosque.marketplace.busqueda.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaResponse;
import co.edu.unbosque.marketplace.busqueda.service.BusquedaService;
import jakarta.validation.Valid;

// La busqueda es publica yno necesita iniciar sesion
@RestController
@RequestMapping("/busqueda")
public class BusquedaController {

    private final BusquedaService busquedaService;

    public BusquedaController(BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
    }

    @GetMapping("/publicaciones")
    public BusquedaResponse buscar(@Valid BusquedaRequest datos) {
        return busquedaService.buscar(datos);
    }
}