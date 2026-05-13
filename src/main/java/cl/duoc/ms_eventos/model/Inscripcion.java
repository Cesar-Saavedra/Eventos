package cl.duoc.ms_eventos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Tabla "inscripciones" en la base de datos ms_eventos.
 *
 * Registra la inscripcion de un jugador a un evento especifico.
 * Es la tabla intermedia de la relacion Muchos a Muchos entre
 * jugadores (en ms-login) y eventos (en esta BD).
 *
 * Un jugador puede inscribirse a muchos eventos.
 * Un evento puede tener muchos jugadores inscritos.
 *
 * El campo "usuarioId" referencia al jugador en ms-login.
 * No es una FK real porque son bases de datos distintas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inscripciones")
public class Inscripcion {

    // Identificador unico autogenerado
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Referencia al evento en esta misma BD
    // @ManyToOne: muchas inscripciones pueden apuntar al mismo evento
    // @JoinColumn: nombre de la columna FK en la tabla inscripciones
    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    // Id del jugador inscrito (viene de ms-login, no FK real)
    @Column(nullable = false)
    private Integer usuarioId;

    // Cuando se realizo la inscripcion (se asigna automaticamente)
    @Column(nullable = false)
    private LocalDateTime fechaInscripcion;

    // Si el jugador confirmo su asistencia al evento
    // false = inscrito pero sin confirmar, true = confirmado
    @Column(nullable = false)
    private Boolean confirmado = false;

}
