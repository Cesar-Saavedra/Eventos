package cl.duoc.ms_eventos.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.duoc.ms_eventos.model.EstadoEvento;
import cl.duoc.ms_eventos.model.Evento;

/*
 * Repositorio que maneja las consultas a la tabla "eventos".
 *
 * JpaRepository ya trae los metodos basicos:
 *   save(evento)      -> INSERT o UPDATE
 *   findById(id)      -> SELECT WHERE id = ?
 *   findAll()         -> SELECT * FROM eventos
 *
 * Los metodos que agregamos siguen la convencion de Spring Data JPA.
 * Spring genera el SQL automaticamente segun el nombre del metodo.
 */
@Repository
public interface EventoRepository extends JpaRepository<Evento, Integer> {

    // Todos los eventos organizados por una tienda especifica
    List<Evento> findByTiendaId(Integer tiendaId);

    // Todos los eventos con un estado especifico
    // Ejemplo: findByEstado(EstadoEvento.ABIERTO) -> solo eventos abiertos
    List<Evento> findByEstado(EstadoEvento estado);

    // Eventos de una tienda con un estado especifico
    // Ejemplo: ver todos los torneos ABIERTOS de la tienda 3
    List<Evento> findByTiendaIdAndEstado(Integer tiendaId, EstadoEvento estado);

    /*
     * Busca el evento y bloquea la fila (SELECT ... FOR UPDATE) hasta que
     * termine la transaccion actual. Se usa al inscribirse para que el
     * conteo de cupos y la insercion de la inscripcion sean atomicos:
     * dos inscripciones concurrentes al ultimo cupo ya no pueden
     * pasar ambas el chequeo de cupos disponibles.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findByIdConBloqueo(@Param("id") Integer id);

}
