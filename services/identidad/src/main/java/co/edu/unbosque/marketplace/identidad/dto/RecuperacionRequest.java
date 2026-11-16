package co.edu.unbosque.marketplace.identidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// HU-04 correo con el que se pide recuperar la contraseña
public class RecuperacionRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    private String correo;

    public RecuperacionRequest() {
    }

    public RecuperacionRequest(String correo) {
        this.correo = correo;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
