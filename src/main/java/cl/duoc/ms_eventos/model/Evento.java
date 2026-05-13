package cl.duoc.ms_eventos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Tabla "eventos" en la base de datos ms_eventos.
 *
 * Representa un evento o torneo organizado por una tienda CardLink.
 * Cada evento tiene cupos limitados y un precio de inscripcion (puede ser 0).
 *
 * El campo "tiendaId" referencia la tienda organizadora en ms-tiendas.
 * No es una FK real porque son bases de datos distintas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "eventos")
public class Evento {

    // Identificador unico autogenerado
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre descriptivo del evento
    // Ejemplo: "Torneo Regional Pokemon - Mayo 2026"
    @Column(nullable = false, length = 200)
    private String nombre;

    // Descripcion detallada: reglas, que llevar, premios, formato
    @Column(length = 1000)
    private String descripcion;

    // Id de la tienda organizadora en ms-tiendas (no FK real)
    @Column(nullable = false)
    private Integer tiendaId;

    // Tipo de evento: TORNEO, PRERELEASE, DRAFT, CASUAL o CAMPEONATO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipoEvento;

    // Estado actual del evento en su ciclo de vida
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estado = EstadoEvento.ABIERTO;

    // Fecha y hora en que empieza el evento
    // LocalDateTime guarda fecha + hora sin zona horaria
    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    // Cantidad maxima de jugadores que pueden inscribirse
    @Column(nullable = false)
    private Integer cuposMaximos;

    // Precio de inscripcion en pesos chilenos
    // 0 significa que el evento es gratuito
    @Column(nullable = false)
    private Integer precioInscripcion = 0;

}
