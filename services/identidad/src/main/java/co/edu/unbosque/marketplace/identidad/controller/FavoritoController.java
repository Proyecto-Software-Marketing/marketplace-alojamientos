package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.FavoritosResponse;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.service.FavoritoService;
import co.edu.unbosque.marketplace.identidad.service.SesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios/favoritos")
public class FavoritoController {

    private static final List<Rol> SOLO_HUESPED = List.of(Rol.HUESPED);

    private final FavoritoService favoritoService;
    private final SesionService sesionService;

    public FavoritoController(FavoritoService favoritoService, SesionService sesionService) {
        this.favoritoService = favoritoService;
        this.sesionService = sesionService;
    }

    @GetMapping
    public ResponseEntity<FavoritosResponse> listar(@RequestHeader("Authorization") String autorizacion) {
        Long idUsuario = idHuesped(autorizacion);
        return ResponseEntity.ok(favoritoService.listar(idUsuario));
    }

    @PostMapping("/{idPublicacion}")
    public ResponseEntity<FavoritosResponse> agregar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable Long idPublicacion) {
        Long idUsuario = idHuesped(autorizacion);
        return ResponseEntity.ok(favoritoService.agregar(idUsuario, idPublicacion));
    }

    @DeleteMapping("/{idPublicacion}")
    public ResponseEntity<FavoritosResponse> quitar(@RequestHeader("Authorization") String autorizacion,
            @PathVariable Long idPublicacion) {
        Long idUsuario = idHuesped(autorizacion);
        return ResponseEntity.ok(favoritoService.quitar(idUsuario, idPublicacion));
    }

    private Long idHuesped(String autorizacion) {
        String token = autorizacion.replace("Bearer ", "").trim();
        return sesionService.autorizar(token, SOLO_HUESPED, null).getId();
    }
}