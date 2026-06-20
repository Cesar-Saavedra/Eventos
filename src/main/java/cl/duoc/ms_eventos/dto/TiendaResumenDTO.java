package cl.duoc.ms_eventos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * DTO que representa el resumen de una tienda recibido desde ms-tiendas.
 * ms-eventos usa el nombre y estado para validar y enriquecer respuestas de eventos.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TiendaResumenDTO {

    private Integer id;
    private String nombre;
    private String horarioAtencion;
    private String estado;
}
