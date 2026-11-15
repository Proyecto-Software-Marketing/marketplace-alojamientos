package co.edu.unbosque.marketplace.resenas.repository;

// Una fila del cálculo de promedios: publicación y promedio de sus reseñas visibles
public interface PromedioPublicacion {

    Long getPublicacionId();

    Double getPromedio();
}
