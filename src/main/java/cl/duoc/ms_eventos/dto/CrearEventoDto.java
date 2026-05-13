package cl.duoc.ms_eventos.dto;

import java.time.LocalDateTime;

import cl.duoc.ms_eventos.model.TipoEvento;
import lombok.Data;

/*
 * DTO de PETICION: datos para crear un nuevo evento.
 *
 * El tiendaId viene en la URL (PathVariable), no en el body.
 * El estado NO viene del cliente: siempre empieza en ABIERTO.
 *
 * Ejemplo de body JSON:
 * {
 *   "nombre": "Torneo Regional Pokemon - Mayo 2026",
 *   "descripcion": "Formato Estandar, traer mazo de 60 cartas. Premio: sobre x5 al ganador.",
 *   "tipoEvento": "TORNEO",
 *   "fechaInicio": "2026-05-30T14:00:00",
 *   "cuposMaximos": 16,
 *   "precioInscripcion": 3000
 * }
 */
@Data
public class CrearEventoDto {

    private String        nombre;
    private String        descripcion;
    private TipoEvento    tipoEvento;
    private LocalDateTime fechaInicio;
    private Integer       cuposMaximos;
    private Integer       precioInscripcion; // null o 0 si es gratis

}
