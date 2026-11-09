package co.edu.unbosque.marketplace.identidad.config;

import co.edu.unbosque.marketplace.identidad.model.Rol;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

// Crea el administrador 
@Component
public class CargaInicial implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();


    @Value("${admin.correo}")
    private String correoAdmin;

    @Value("${admin.contrasena}")
    private String contrasenaAdmin;

    public CargaInicial(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {

        if (correoAdmin.isBlank() || contrasenaAdmin.isBlank()) {
            return;
        }

        String correo = correoAdmin.trim().toLowerCase();

      
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }

        usuarioRepository.save(new Usuario("Administrador", correo, cifrador.encode(contrasenaAdmin), Rol.ADMINISTRADOR));
    }
}