package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.RegistroRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.RegistroException;
import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

// Reglas de negocio de los usuarios

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

   // Registra un Huesped o un Anfitrion HU-01
    public UsuarioResponse registrar(RegistroRequest datos) {
        // CA-003
        if (datos.getRol() == Rol.ADMINISTRADOR) {
            throw new RegistroException("No es posible registrarse como Administrador");
        }

        // CA-002
        String correo = datos.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RegistroException("El correo ya está en uso");
        }

        // CA-001 y CA-127: cuenta con un único rol y la contraseña cifrada
        String passwordCifrada = cifrador.encode(datos.getContrasena());
        Usuario usuario = new Usuario(datos.getNombre().trim(), correo, passwordCifrada, datos.getRol());
        Usuario guardado = usuarioRepository.save(usuario);

        return new UsuarioResponse(guardado);
    }
}