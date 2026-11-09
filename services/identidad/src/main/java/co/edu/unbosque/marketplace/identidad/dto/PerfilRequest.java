package co.edu.unbosque.marketplace.identidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Actualizacion de datos en el perfil
public class PerfilRequest {

    // CA-015
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    // CA-015
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    private String correo;

    public PerfilRequest() {
    }

    public PerfilRequest(String nombre, String correo) {
        this.nombre = nombre;
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}