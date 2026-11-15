package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.model.Resena;

import java.util.List;

// Resultado de consultar las reseñas visibles de una publicación (HU-33)
public record ResenasPublicacion(List<Resena> resenas, Double calificacionPromedio) {
}
