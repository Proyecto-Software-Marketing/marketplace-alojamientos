package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.Sesion;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

//Inicio y cierre de sesion

@Service
public class SesionService {

    
    private static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    private static final String SESION_NO_VALIDA = "La sesión no es válida o ya fue cerrada";

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public SesionService(UsuarioRepository usuarioRepository, SesionRepository sesionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
    }

    // CA-004
    public SesionResponse iniciarSesion(LoginRequest datos) {
        String correo = datos.getCorreo().trim().toLowerCase();

        // CA-005
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new AutenticacionException(CREDENCIALES_INCORRECTAS));

        // CA-005
        if (!cifrador.matches(datos.getContrasena(), usuario.getContrasena())) {
            throw new AutenticacionException(CREDENCIALES_INCORRECTAS);
        }

        // CA-004
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new AutenticacionException("La cuenta no está activa");
        }

        String token = UUID.randomUUID().toString();
        sesionRepository.save(new Sesion(token, usuario));

        return new SesionResponse(token, new UsuarioResponse(usuario));
    }

    // CA-006: al cerrar sesión el token deja de servir
     
    public void cerrarSesion(String token) {
        Sesion sesion = sesionRepository.findByTokenAndActivaTrue(token)
                .orElseThrow(() -> new AutenticacionException(SESION_NO_VALIDA));
        sesion.setActiva(false);
        sesionRepository.save(sesion);
    }

   //Revisa que el token pertenezca a una sesión activa y devuelve el usuario.
//Es lo que protege las rutas, es decir sin sesión activa no se puede entrar 
    
    public UsuarioResponse validarSesion(String token) {
        Sesion sesion = sesionRepository.findByTokenAndActivaTrue(token)
                .orElseThrow(() -> new AutenticacionException(SESION_NO_VALIDA));
        return new UsuarioResponse(sesion.getUsuario());
    }
}