package co.edu.unbosque.marketplace.busqueda.controller;
import java.util.List;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.busqueda.dto.BusquedaRequest;
import co.edu.unbosque.marketplace.busqueda.dto.BusquedaResponse;
import co.edu.unbosque.marketplace.busqueda.dto.DetalleResponse;
import co.edu.unbosque.marketplace.busqueda.dto.PublicacionResumen;
import co.edu.unbosque.marketplace.busqueda.service.BusquedaService;
import jakarta.validation.Valid;


// no necesita iniciar sesion
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
    @GetMapping("/publicaciones/destacadas")
    public List<PublicacionResumen> destacadas() {
        return busquedaService.destacadas();
    }

    @GetMapping("/publicaciones/{id}")
    public DetalleResponse detalle(@PathVariable("id") Long idPublicacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate llegada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam(required = false) Integer huespedes) {
        return busquedaService.detalle(idPublicacion, llegada, salida, huespedes);
    }
}