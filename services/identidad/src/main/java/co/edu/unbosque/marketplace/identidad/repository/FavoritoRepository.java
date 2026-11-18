package co.edu.unbosque.marketplace.identidad.repository;

import co.edu.unbosque.marketplace.identidad.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
   
    List<Favorito> findByIdUsuarioOrderByIdDesc(Long idUsuario);

    boolean existsByIdUsuarioAndIdPublicacion(Long idUsuario, Long idPublicacion);

    void deleteByIdUsuarioAndIdPublicacion(Long idUsuario, Long idPublicacion);
}