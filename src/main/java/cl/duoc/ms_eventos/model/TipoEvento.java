package cl.duoc.ms_eventos.model;

/*
 * Tipos de eventos que puede organizar una tienda en CardLink.
 * Se usa en la entidad Evento para clasificar que tipo de actividad es.
 */
public enum TipoEvento {
    TORNEO,       // Competencia con eliminacion, ranking y premios
    PRERELEASE,   // Evento de lanzamiento de un nuevo set de cartas
    DRAFT,        // Formato de juego donde se abren sobres y se arman mazos en vivo
    CASUAL,       // Partidas libres sin competencia ni ranking
    CAMPEONATO    // Torneo de alto nivel con mayores premios y requisitos
}
