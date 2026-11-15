package co.edu.unbosque.marketplace.resenas.service;

import co.edu.unbosque.marketplace.resenas.model.Resena;

// Resultado de ocultar una reseña (HU-36). recienOcultada es false si ya estaba oculta (decisión 4),
// y en ese caso no se envía otro evento a Auditoría
public record ResultadoOcultamiento(Resena resena, boolean recienOcultada) {
}
