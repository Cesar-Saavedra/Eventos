package cl.duoc.ms_eventos.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * DTO de RESPUESTA: datos de una inscripcion de un jugador a un evento.
 *
 * Ejemplo de respuesta JSON:
 * {
 *   "id": 5,
 *   "eventoId": 1,
 *   "nombreEvento": "Torneo Regional Pokemon - Mayo 2026",
 *   "usuarioId": 7,
 *   "nombreUsuario": "DarkMage99",
 *   "fechaInscripcion": "2026-05-10T18:30:00",
 *   "confirmado": false
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionRespuestaDto {

    private Integer       id;
    private Integer       eventoId;
    private String        nombreEvento;
    private Integer       usuarioId;
    private String        nombreUsuario;   // nombre del jugador
    private LocalDateTime fechaInscripcion;
    private Boolean       confirmado;

}
