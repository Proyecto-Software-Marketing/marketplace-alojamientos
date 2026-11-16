package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.client.ServiciosExternos;
import co.edu.unbosque.marketplace.identidad.dto.RecuperacionRequest;
import co.edu.unbosque.marketplace.identidad.dto.RestablecerRequest;
import co.edu.unbosque.marketplace.identidad.dto.UsuarioResponse;
import co.edu.unbosque.marketplace.identidad.exception.ServicioNoDisponibleException;
import co.edu.unbosque.marketplace.identidad.exception.TokenRecuperacionException;
import co.edu.unbosque.marketplace.identidad.model.EstadoUsuario;
import co.edu.unbosque.marketplace.identidad.model.RecuperacionContrasena;
import co.edu.unbosque.marketplace.identidad.model.Sesion;
import co.edu.unbosque.marketplace.identidad.model.Usuario;
import co.edu.unbosque.marketplace.identidad.repository.RecuperacionContrasenaRepository;
import co.edu.unbosque.marketplace.identidad.repository.SesionRepository;
import co.edu.unbosque.marketplace.identidad.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// HU-04 recuperación simulada de contraseña: el token se muestra en una notificación interna
@Service
public class RecuperacionService {

    private static final String TOKEN_NO_VALIDO = "Token vencido o ya usado";

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final RecuperacionContrasenaRepository recuperacionRepository;
    private final ServiciosExternos serviciosExternos;
    private final Clock reloj;
    private final long minutosVigencia;
    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

    public RecuperacionService(UsuarioRepository usuarioRepository, SesionRepository sesionRepository,
            RecuperacionContrasenaRepository recuperacionRepository, ServiciosExternos serviciosExternos,
            Clock reloj, @Value("${identidad.recuperacion.minutos-vigencia}") long minutosVigencia) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.serviciosExternos = serviciosExternos;
        this.reloj = reloj;
        this.minutosVigencia = minutosVigencia;
    }

    // CA-010 genera el token y lo muestra en una notificación interna, sin correo real
    @Transactional
    public void solicitar(RecuperacionRequest datos) {
        String correo = datos.getCorreo().trim().toLowerCase();

        // Decisión 3 si el correo no existe no se hace nada, la respuesta es igual para no revelarlo
        // Decisión 4 una cuenta restringida no puede entrar, entonces no se genera token
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(correo);
        if (encontrado.isEmpty() || encontrado.get().getEstado() == EstadoUsuario.RESTRINGIDO) {
            return;
        }
        Usuario usuario = encontrado.get();

        // Decisión 5 los tokens que pidió antes y no usó dejan de servir
        List<RecuperacionContrasena> anteriores = recuperacionRepository.findByUsuarioIdAndUsadoFalse(usuario.getId());
        anteriores.forEach(anterior -> anterior.setUsado(true));
        recuperacionRepository.saveAll(anteriores);

        // Decisiones 1 y 2 token al azar que vence en 30 minutos
        String token = UUID.randomUUID().toString();
        LocalDateTime vence = LocalDateTime.now(reloj).plusMinutes(minutosVigencia);
        RecuperacionContrasena recuperacion = recuperacionRepository.save(
                new RecuperacionContrasena(token, usuario, vence));

        // Decisión 8 sin la notificación el usuario no ve el token, entonces se revierte todo
        String mensaje = "Tu token para recuperar la contraseña es: " + token
                + ". Vence en " + minutosVigencia + " minutos.";
        try {
            serviciosExternos.notificarRecuperacion(usuario.getId(), recuperacion.getId(), mensaje);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(
                    "No fue posible generar la notificación de recuperación. Intente más tarde");
        }
    }

    // CA-011 con un token válido se cambia la contraseña y el token deja de servir
    @Transactional
    public void restablecer(RestablecerRequest datos) {
        // CA-012 y Decisión 6 vencido, usado o inexistente se rechaza con el mismo mensaje
        RecuperacionContrasena recuperacion = recuperacionRepository.findByToken(datos.getToken().trim())
                .filter(encontrada -> !encontrada.isUsado())
                .filter(encontrada -> LocalDateTime.now(reloj).isBefore(encontrada.getFechaVencimiento()))
                .orElseThrow(() -> new TokenRecuperacionException(TOKEN_NO_VALIDO));

        Usuario usuario = recuperacion.getUsuario();
        usuario.setContrasena(cifrador.encode(datos.getNuevaContrasena()));
        usuarioRepository.save(usuario);

        recuperacion.setUsado(true);
        recuperacionRepository.save(recuperacion);

        // Decisión 7 se cierran las sesiones abiertas con la contraseña anterior
        List<Sesion> sesiones = sesionRepository.findByUsuarioIdAndActivaTrue(usuario.getId());
        sesiones.forEach(sesion -> sesion.setActiva(false));
        sesionRepository.saveAll(sesiones);
    }

    // Decisión 10 Notificaciones pregunta de quién es el correo para el buzón simulado
    @Transactional(readOnly = true)
    public Optional<UsuarioResponse> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase()).map(UsuarioResponse::new);
    }
}
