package cl.duoc.ms_eventos.model;

/*
 * Estados del ciclo de vida de un evento.
 *
 * El flujo normal es:
 * ABIERTO -> CERRADO -> EN_CURSO -> FINALIZADO
 *
 * Un evento puede pasar de ABIERTO a CANCELADO en cualquier momento.
 */
public enum EstadoEvento {
    ABIERTO,     // El evento acepta inscripciones de jugadores
    CERRADO,     // Las inscripciones cerraron, el evento aun no empieza
    EN_CURSO,    // El evento esta ocurriendo en este momento
    FINALIZADO,  // El evento termino exitosamente
    CANCELADO    // El evento fue cancelado por la tienda organizadora
}
