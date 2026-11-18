package co.edu.unbosque.marketplace.identidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


// Solo se guardan los ids, los datos de la publicación se le piden a Busqueda
@Entity
@Table(name = "favorito", uniqueConstraints = @UniqueConstraint(columnNames = { "id_usuario", "id_publicacion" }))
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_publicacion", nullable = false)
    private Long idPublicacion;

    public Favorito() {
    }

    public Favorito(Long idUsuario, Long idPublicacion) {
        this.idUsuario = idUsuario;
        this.idPublicacion = idPublicacion;
    }

    public Long getId() {
        return id;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public Long getIdPublicacion() {
        return idPublicacion;
    }
}