package cl.duoc.ms_eventos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.ms_eventos.client.TiendaFeignClient;
import cl.duoc.ms_eventos.client.UsuarioFeignClient;
import cl.duoc.ms_eventos.dto.CrearEventoDto;
import cl.duoc.ms_eventos.dto.EventoRespuestaDto;
import cl.duoc.ms_eventos.dto.InscripcionRespuestaDto;
import cl.duoc.ms_eventos.dto.TiendaResumenDTO;
import cl.duoc.ms_eventos.model.EstadoEvento;
import cl.duoc.ms_eventos.model.Evento;
import cl.duoc.ms_eventos.model.Inscripcion;
import cl.duoc.ms_eventos.repository.EventoRepository;
import cl.duoc.ms_eventos.repository.InscripcionRepository;

/*
 * Servicio con toda la logica de negocio de ms-eventos.
 *
 * Arquitectura: Controller -> Service -> Repository (CSR)
 *
 * Este servicio se comunica con otros dos microservicios via Feign:
 *
 * 1. ms-tiendas (TiendaFeignClient): verificar que la tienda organizadora
 *    existe y esta ACTIVA, obtener tiendas por dueno, notificar nuevo evento.
 *
 * 2. ms-usuarios (UsuarioFeignClient): notificar cuando un jugador se inscribe,
 *    para que ms-usuarios actualice el contador de eventos del perfil.
 *
 * Feign usa Eureka para resolver los hosts sin URLs hardcodeadas en el yaml.
 */
@Service
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    // Cliente Feign para comunicarse con ms-tiendas via Eureka
    @Autowired
    private TiendaFeignClient tiendaFeignClient;

    // Cliente Feign para comunicarse con ms-usuarios via Eureka
    @Autowired
    private UsuarioFeignClient usuarioFeignClient;

    // =========================================================
    // CREAR EVENTO
    // =========================================================

    public EventoRespuestaDto crearEvento(Integer tiendaId, CrearEventoDto dto, String authHeader) {

        // Paso 1: verificar que la tienda existe y esta ACTIVA
        TiendaResumenDTO datosTienda = consultarResumenTienda(tiendaId, authHeader);
        if (datosTienda == null) {
            throw new RuntimeException("No se encontro la tienda con id: " + tiendaId
                    + ". Verifica que ms-tiendas este corriendo.");
        }
        if (!"ACTIVA".equals(datosTienda.getEstado())) {
            throw new RuntimeException("La tienda no esta activa. Estado actual: " + datosTienda.getEstado()
                    + ". Solo las tiendas ACTIVAS pueden crear eventos.");
        }

        // Paso 2: validar datos obligatorios
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del evento es obligatorio.");
        }
        if (dto.getFechaInicio() == null) {
            throw new RuntimeException("La fecha de inicio del evento es obligatoria.");
        }
        if (dto.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La fecha de inicio debe ser en el futuro.");
        }
        if (dto.getCuposMaximos() == null || dto.getCuposMaximos() <= 0) {
            throw new RuntimeException("Los cupos maximos deben ser un numero mayor a cero.");
        }
        if (dto.getTipoEvento() == null) {
            throw new RuntimeException("Debes seleccionar un tipo de evento.");
        }

        // Paso 3: crear el evento y guardarlo en la BD
        Evento nuevoEvento = new Evento();
        nuevoEvento.setNombre(dto.getNombre());
        nuevoEvento.setDescripcion(dto.getDescripcion());
        nuevoEvento.setTiendaId(tiendaId);
        nuevoEvento.setTipoEvento(dto.getTipoEvento());
        nuevoEvento.setEstado(EstadoEvento.ABIERTO);
        nuevoEvento.setFechaInicio(dto.getFechaInicio());
        nuevoEvento.setCuposMaximos(dto.getCuposMaximos());
        nuevoEvento.setPrecioInscripcion(dto.getPrecioInscripcion() != null ? dto.getPrecioInscripcion() : 0);
        Evento eventoGuardado = eventoRepository.save(nuevoEvento);

        // Paso 4: notificar a ms-tiendas (fire and forget)
        notificarNuevoEventoATiendas(tiendaId, authHeader);

        return construirRespuestaEvento(eventoGuardado, datosTienda.getNombre(), eventoGuardado.getCuposMaximos());
    }

    // =========================================================
    // LISTAR EVENTOS ABIERTOS
    // =========================================================

    public List<EventoRespuestaDto> listarEventosAbiertos(String authHeader) {

        List<Evento> eventosAbiertos = eventoRepository.findByEstado(EstadoEvento.ABIERTO);
        List<EventoRespuestaDto> listaRespuesta = new ArrayList<>();

        for (Evento evento : eventosAbiertos) {
            TiendaResumenDTO datosTienda = consultarResumenTienda(evento.getTiendaId(), authHeader);
            String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";
            int inscritos = inscripcionRepository.countByEvento(evento);
            listaRespuesta.add(construirRespuestaEvento(evento, nombreTienda, evento.getCuposMaximos() - inscritos));
        }

        return listaRespuesta;
    }

    // =========================================================
    // LISTAR EVENTOS DE UNA TIENDA
    // =========================================================

    public List<EventoRespuestaDto> listarEventosDeTienda(Integer tiendaId, String authHeader) {

        List<Evento> eventos = eventoRepository.findByTiendaId(tiendaId);
        TiendaResumenDTO datosTienda = consultarResumenTienda(tiendaId, authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";

        List<EventoRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Evento evento : eventos) {
            int inscritos = inscripcionRepository.countByEvento(evento);
            listaRespuesta.add(construirRespuestaEvento(evento, nombreTienda, evento.getCuposMaximos() - inscritos));
        }
        return listaRespuesta;
    }

    // =========================================================
    // VER UN EVENTO POR ID
    // =========================================================

    public EventoRespuestaDto obtenerPorId(Integer id, String authHeader) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));
        TiendaResumenDTO datosTienda = consultarResumenTienda(evento.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";
        int inscritos = inscripcionRepository.countByEvento(evento);
        return construirRespuestaEvento(evento, nombreTienda, evento.getCuposMaximos() - inscritos);
    }

    // =========================================================
    // CAMBIAR ESTADO DE UN EVENTO
    // =========================================================

    public EventoRespuestaDto cambiarEstado(Integer id, EstadoEvento nuevoEstado,
                                             Integer usuarioId, String authHeader) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));

        if (!esTiendaDelUsuario(evento.getTiendaId(), usuarioId, authHeader)) {
            throw new RuntimeException("No tienes permiso para modificar este evento.");
        }

        evento.setEstado(nuevoEstado);
        Evento eventoActualizado = eventoRepository.save(evento);

        TiendaResumenDTO datosTienda = consultarResumenTienda(evento.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null ? datosTienda.getNombre() : "Tienda desconocida";
        int inscritos = inscripcionRepository.countByEvento(eventoActualizado);
        return construirRespuestaEvento(eventoActualizado, nombreTienda, eventoActualizado.getCuposMaximos() - inscritos);
    }

    // =========================================================
    // INSCRIBIRSE A UN EVENTO
    // =========================================================

    /*
     * @Transactional + bloqueo pesimista sobre el evento: dos inscripciones
     * concurrentes al mismo evento quedan serializadas, por lo que el conteo
     * de cupos y el guardado de la inscripcion son atomicos (ya no se puede
     * sobre-inscribir el ultimo cupo con requests en paralelo). La restriccion
     * unica (evento_id, usuarioId) en la tabla actua como segunda barrera
     * contra la doble inscripcion.
     */
    @Transactional
    public InscripcionRespuestaDto inscribirse(Integer eventoId, Integer usuarioId,
                                                String nombre, String authHeader) {

        Evento evento = eventoRepository.findByIdConBloqueo(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventoId));

        if (!EstadoEvento.ABIERTO.equals(evento.getEstado())) {
            throw new RuntimeException("Este evento no acepta inscripciones. Estado: " + evento.getEstado());
        }
        if (inscripcionRepository.findByEventoAndUsuarioId(evento, usuarioId).isPresent()) {
            throw new RuntimeException("Ya estas inscrito en este evento.");
        }
        if (inscripcionRepository.countByEvento(evento) >= evento.getCuposMaximos()) {
            throw new RuntimeException("Lo sentimos, no quedan cupos disponibles.");
        }

        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setEvento(evento);
        nuevaInscripcion.setUsuarioId(usuarioId);
        nuevaInscripcion.setFechaInscripcion(LocalDateTime.now());
        nuevaInscripcion.setConfirmado(false);

        Inscripcion guardada;
        try {
            guardada = inscripcionRepository.save(nuevaInscripcion);
        } catch (DataIntegrityViolationException e) {
            // Respaldo si dos requests pasaron el check de arriba casi al mismo tiempo
            throw new RuntimeException("Ya estas inscrito en este evento.");
        }

        // Notificar a ms-usuarios (fire and forget)
        notificarInscripcionAUsuarios(usuarioId, authHeader);

        return construirRespuestaInscripcion(guardada, nombre);
    }

    // =========================================================
    // MIS INSCRIPCIONES
    // =========================================================

    public List<InscripcionRespuestaDto> misInscripciones(Integer usuarioId, String nombre) {
        List<InscripcionRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Inscripcion inscripcion : inscripcionRepository.findByUsuarioId(usuarioId)) {
            listaRespuesta.add(construirRespuestaInscripcion(inscripcion, nombre));
        }
        return listaRespuesta;
    }

    // =========================================================
    // VER PARTICIPANTES
    // =========================================================

    public List<InscripcionRespuestaDto> verParticipantes(Integer eventoId, Integer usuarioId, String authHeader) {

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventoId));

        if (!esTiendaDelUsuario(evento.getTiendaId(), usuarioId, authHeader)) {
            throw new RuntimeException("Solo la tienda organizadora puede ver los participantes.");
        }

        List<InscripcionRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Inscripcion inscripcion : inscripcionRepository.findByEvento(evento)) {
            listaRespuesta.add(construirRespuestaInscripcion(inscripcion,
                    "Jugador #" + inscripcion.getUsuarioId()));
        }
        return listaRespuesta;
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    private TiendaResumenDTO consultarResumenTienda(Integer tiendaId, String authHeader) {
        try {
            return tiendaFeignClient.obtenerResumenTienda(tiendaId, authHeader);
        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo consultar ms-tiendas para tienda "
                    + tiendaId + ": " + e.getMessage());
            return null;
        }
    }

    private boolean esTiendaDelUsuario(Integer tiendaId, Integer usuarioId, String authHeader) {
        try {
            List<TiendaResumenDTO> tiendas = tiendaFeignClient.obtenerTiendasPorDueno(usuarioId, authHeader);
            if (tiendas == null) return false;
            for (TiendaResumenDTO tienda : tiendas) {
                if (tiendaId.equals(tienda.getId())) return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo verificar dueno de tienda "
                    + tiendaId + ": " + e.getMessage());
            return false;
        }
    }

    private void notificarNuevoEventoATiendas(Integer tiendaId, String authHeader) {
        try {
            tiendaFeignClient.sumarEvento(tiendaId, authHeader);
        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo notificar a ms-tiendas: " + e.getMessage());
        }
    }

    private void notificarInscripcionAUsuarios(Integer usuarioId, String authHeader) {
        try {
            usuarioFeignClient.sumarEvento(usuarioId, authHeader);
        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo notificar a ms-usuarios: " + e.getMessage());
        }
    }

    private EventoRespuestaDto construirRespuestaEvento(Evento evento, String nombreTienda, int cuposDisponibles) {
        EventoRespuestaDto respuesta = new EventoRespuestaDto();
        respuesta.setId(evento.getId());
        respuesta.setNombre(evento.getNombre());
        respuesta.setDescripcion(evento.getDescripcion());
        respuesta.setTiendaId(evento.getTiendaId());
        respuesta.setNombreTienda(nombreTienda);
        respuesta.setTipoEvento(evento.getTipoEvento());
        respuesta.setEstado(evento.getEstado());
        respuesta.setFechaInicio(evento.getFechaInicio());
        respuesta.setCuposMaximos(evento.getCuposMaximos());
        respuesta.setCuposDisponibles(cuposDisponibles);
        respuesta.setPrecioInscripcion(evento.getPrecioInscripcion());
        return respuesta;
    }

    private InscripcionRespuestaDto construirRespuestaInscripcion(Inscripcion inscripcion, String nombreUsuario) {
        InscripcionRespuestaDto respuesta = new InscripcionRespuestaDto();
        respuesta.setId(inscripcion.getId());
        respuesta.setEventoId(inscripcion.getEvento().getId());
        respuesta.setNombreEvento(inscripcion.getEvento().getNombre());
        respuesta.setUsuarioId(inscripcion.getUsuarioId());
        respuesta.setNombreUsuario(nombreUsuario);
        respuesta.setFechaInscripcion(inscripcion.getFechaInscripcion());
        respuesta.setConfirmado(inscripcion.getConfirmado());
        return respuesta;
    }
}
