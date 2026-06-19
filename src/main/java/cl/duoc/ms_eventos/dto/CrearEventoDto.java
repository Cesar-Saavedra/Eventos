package cl.duoc.ms_eventos.dto;

import java.time.LocalDateTime;

import cl.duoc.ms_eventos.model.TipoEvento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotBlank(message = "El nombre del evento es obligatorio")
    private String        nombre;

    private String        descripcion;

    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEvento    tipoEvento;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser en el futuro")
    private LocalDateTime fechaInicio;

    @NotNull(message = "Los cupos maximos son obligatorios")
    @Positive(message = "Los cupos maximos deben ser mayor que 0")
    private Integer       cuposMaximos;

    @PositiveOrZero(message = "El precio de inscripcion no puede ser negativo")
    private Integer       precioInscripcion; // null o 0 si es gratis

}
