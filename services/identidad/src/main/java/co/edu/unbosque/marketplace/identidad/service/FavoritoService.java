package co.edu.unbosque.marketplace.identidad.service;

import co.edu.unbosque.marketplace.identidad.dto.FavoritosResponse;
import co.edu.unbosque.marketplace.identidad.model.Favorito;
import co.edu.unbosque.marketplace.identidad.repository.FavoritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;

    public FavoritoService(FavoritoRepository favoritoRepository) {
        this.favoritoRepository = favoritoRepository;
    }

   
    public FavoritosResponse listar(Long idUsuario) {
        List<Long> publicaciones = favoritoRepository.findByIdUsuarioOrderByIdDesc(idUsuario).stream()
                .map(Favorito::getIdPublicacion)
                .toList();
        return new FavoritosResponse(publicaciones);
    }

  
    public FavoritosResponse agregar(Long idUsuario, Long idPublicacion) {
        if (!favoritoRepository.existsByIdUsuarioAndIdPublicacion(idUsuario, idPublicacion)) {
            favoritoRepository.save(new Favorito(idUsuario, idPublicacion));
        }
        return listar(idUsuario);
    }

    @Transactional
    public FavoritosResponse quitar(Long idUsuario, Long idPublicacion) {
        favoritoRepository.deleteByIdUsuarioAndIdPublicacion(idUsuario, idPublicacion);
        return listar(idUsuario);
    }
}