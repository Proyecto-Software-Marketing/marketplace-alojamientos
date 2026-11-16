package co.edu.unbosque.marketplace.alojamientos.model;

// Estados de una publicación (ListingStatus en el contrato). Toda publicación nace en BORRADOR (CA-020)
public enum EstadoPublicacion {
    BORRADOR,
    PENDIENTE_REVISION,
    ACTIVA,
    PAUSADA,
    BLOQUEADA
}
