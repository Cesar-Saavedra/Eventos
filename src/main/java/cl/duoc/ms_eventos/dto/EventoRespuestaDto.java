package cl.duoc.ms_eventos.dto;

import java.time.LocalDateTime;

import cl.duoc.ms_eventos.model.EstadoEvento;
import cl.duoc.ms_eventos.model.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * DTO de RESPUESTA: lo que devuelve el servidor con los datos de un evento.
 *
 * Incluye "nombreTienda" enriquecido desde ms-tiendas
 * y "cuposDisponibles" calculado al momento de la consulta
 * (cuposMaximos - inscritosActuales).
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id": 1,
 *   "nombre": "Torneo Regional Pokemon - Mayo 2026",
 *   "descripcion": "Formato Estandar...",
 *   "tiendaId": 3,
 *   "nombreTienda": "Carta Magica TCG",
 *   "tipoEvento": "TORNEO",
 *   "estado": "ABIERTO",
 *   "fechaInicio": "2026-05-30T14:00:00",
 *   "cuposMaximos": 16,
 *   "cuposDisponibles": 10,
 *   "precioInscripcion": 3000
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoRespuestaDto {

    private Integer       id;
    private String        nombre;
    private String        descripcion;
    private Integer       tiendaId;
    private String        nombreTienda;     // enriquecido desde ms-tiendas
    private TipoEvento    tipoEvento;
    private EstadoEvento  estado;
    private LocalDateTime fechaInicio;
    private Integer       cuposMaximos;
    private Integer       cuposDisponibles; // calculado al momento de la consulta
    private Integer       precioInscripcion;

}
