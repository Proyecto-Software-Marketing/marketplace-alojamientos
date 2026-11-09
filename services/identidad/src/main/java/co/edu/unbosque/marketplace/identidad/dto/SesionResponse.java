package co.edu.unbosque.marketplace.identidad.dto;

public class SesionResponse {

    private String token;
    private UsuarioResponse usuario;

    public SesionResponse(String token, UsuarioResponse usuario) {
        this.token = token;
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public UsuarioResponse getUsuario() {
        return usuario;
    }
}