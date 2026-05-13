package cl.duoc.ms_eventos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.ms_eventos.model.Evento;
import cl.duoc.ms_eventos.model.Inscripcion;

/*
 * Repositorio que maneja las consultas a la tabla "inscripciones".
 */
@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    // Todas las inscripciones de un evento especifico
    // Se usa para listar los participantes y contar cupos usados
    List<Inscripcion> findByEvento(Evento evento);

    // Todos los eventos a los que se inscribio un usuario
    // Se usa para que el jugador vea su historial de torneos
    List<Inscripcion> findByUsuarioId(Integer usuarioId);

    // Busca si un usuario ya esta inscrito en un evento especifico
    // Devuelve Optional porque puede que no exista la inscripcion
    Optional<Inscripcion> findByEventoAndUsuarioId(Evento evento, Integer usuarioId);

    // Cuenta cuantos jugadores estan inscritos en un evento
    // Se usa para calcular los cupos disponibles
    int countByEvento(Evento evento);

}
