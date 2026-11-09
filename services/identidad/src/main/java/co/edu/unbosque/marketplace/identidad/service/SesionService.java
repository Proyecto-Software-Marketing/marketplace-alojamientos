package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.LoginRequest;
import co.edu.unbosque.marketplace.identidad.dto.SesionResponse;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.AutenticacionException;
import co.edu.unbosque.marketplace.identidad.exception.PermisoException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.model.Sesion;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

//  Maneja el inicio y cierre de sesión y quien puede hacer qué
@Service
public class SesionService {

    // Mismo mensaje si falla el correo o la contraseña
    private static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    private static final String SESION_NO_VALIDA = "La sesión no es válida o ya fue cerrada";

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public SesionService(UsuarioRepository usuarioRepository, SesionRepository sesionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
    }

    // Inicia sesión y devuelve el token con los datos del usuario
    public SesionResponse iniciarSesion(LoginRequest datos) {
        String correo = datos.getCorreo().trim().toLowerCase();

        // CA-005 si el correo no existe
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new AutenticacionException(CREDENCIALES_INCORRECTAS));

        // CA-005 si la contraseña no coincide con la guardada
        if (!cifrador.matches(datos.getContrasena(), usuario.getContrasena())) {
            throw new AutenticacionException(CREDENCIALES_INCORRECTAS);
        }

        // CA-004 solo entran cuentas activas
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new AutenticacionException("La cuenta no está activa");
        }

        // Se crea un token al azar y se guarda la sesion
        String token = UUID.randomUUID().toString();
        sesionRepository.save(new Sesion(token, usuario));

        // CA-004
        return new SesionResponse(token, new UsuarioResponse(usuario));
    }

    // CA-006 cerrar sesion deja el token sin servir
    public void cerrarSesion(String token) {
        Sesion sesion = sesionRepository.findByTokenAndActivaTrue(token)
                .orElseThrow(() -> new AutenticacionException(SESION_NO_VALIDA));
        sesion.setActiva(false);
        sesionRepository.save(sesion);
    }

    // CA-006 y CA-008 revisa que el token sea de una sesion abierta y devuelve el usuario
    public UsuarioResponse validarSesion(String token) {
        Sesion sesion = sesionRepository.findByTokenAndActivaTrue(token)
                .orElseThrow(() -> new AutenticacionException(SESION_NO_VALIDA));
        return new UsuarioResponse(sesion.getUsuario());
    }

    // Revisa si el usuario puede hacer algo segun su rol y si es el dueño
    public UsuarioResponse autorizar(String token, List<Rol> rolesPermitidos, Long idDueno) {
        UsuarioResponse usuario = validarSesion(token);

        // CA-007
        if (!rolesPermitidos.contains(usuario.getRol())) {
            throw new PermisoException("No tiene permiso para realizar esta operación");
        }

        // CA-009 solo si mandan el id del dueño
        if (idDueno != null && !idDueno.equals(usuario.getId())) {
            throw new PermisoException("Solo el dueño puede realizar esta operación");
        }

        return usuario;
    }
}