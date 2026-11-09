package co.edu.unbosque.marketplace.identidad.controller;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.service.SesionService;
import co.edu.unbosque.marketplace.identidad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;


 // Rutas de registro, inicio de sesión y cierre
@RestController
@RequestMapping("/autenticacion")
public class AutenticacionController {

    private final UsuarioService usuarioService;
    private final SesionService sesionService;

    public AutenticacionController(UsuarioService usuarioService, SesionService sesionService) {
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
    }

    // Registro de Huesped o Anfitrión 
    //HU-01
    
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        UsuarioResponse usuario = usuarioService.registrar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    //Inicio de sesión 
    // HU-02, CA-004 y CA-005
   
    @PostMapping("/login")
    public ResponseEntity<SesionResponse> iniciarSesion(@Valid @RequestBody LoginRequest datos) {
        return ResponseEntity.ok(sesionService.iniciarSesion(datos));
    }

    //Cerrar sesión 
    //HU-02 y CA-006
     
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(@RequestHeader("Authorization") String autorizacion) {
        sesionService.cerrarSesion(extraerToken(autorizacion));
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping("/sesion")
    public ResponseEntity<UsuarioResponse> consultarSesion(@RequestHeader("Authorization") String autorizacion) {
        return ResponseEntity.ok(sesionService.validarSesion(extraerToken(autorizacion)));
    }

    // El front enviara un mensaje y esto se encarga de quitar la palbra "Bearer "  
    private String extraerToken(String autorizacion) {
        return autorizacion.replace("Bearer ", "").trim();
    }
    // CA-007 y CA-009 los otros servicios preguntan  si el usuario puede hacer algo
    @GetMapping("/autorizar")
    public ResponseEntity<UsuarioResponse> autorizar(@RequestHeader("Authorization") String autorizacion,
            @RequestParam List<Rol> roles,
            @RequestParam(required = false) Long idDueno) {
        return ResponseEntity.ok(sesionService.autorizar(extraerToken(autorizacion), roles, idDueno));
    }
    // CA-019 se usa correo y contraseña porque con la cuenta inactiva no hay sesión
    @PostMapping("/reactivar")
    public ResponseEntity<SesionResponse> reactivarCuenta(@Valid @RequestBody LoginRequest datos) {
        return ResponseEntity.ok(sesionService.reactivarCuenta(datos));
    }
}