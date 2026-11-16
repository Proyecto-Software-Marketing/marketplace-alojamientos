package co.edu.unbosque.marketplace.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// HU-04 token de la notificación y la contraseña nueva
public class RestablecerRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    // Mismas reglas que en el registro
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String nuevaContrasena;

    public RestablecerRequest() {
    }

    public RestablecerRequest(String token, String nuevaContrasena) {
        this.token = token;
        this.nuevaContrasena = nuevaContrasena;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNuevaContrasena() {
        return nuevaContrasena;
    }

    public void setNuevaContrasena(String nuevaContrasena) {
        this.nuevaContrasena = nuevaContrasena;
    }
}
