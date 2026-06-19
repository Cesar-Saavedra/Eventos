package cl.duoc.ms_eventos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cl.duoc.ms_eventos.dto.CrearEventoDto;
import cl.duoc.ms_eventos.dto.EventoRespuestaDto;
import cl.duoc.ms_eventos.dto.InscripcionRespuestaDto;
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
 * Este servicio se comunica con otros dos microservicios:
 *
 * 1. ms-tiendas: para verificar que la tienda organizadora existe
 *    y esta ACTIVA antes de crear un evento.
 *    Tambien notifica a ms-tiendas cuando se crea un evento nuevo
 *    para que actualice su contador de eventos en las metricas.
 *
 * 2. ms-usuarios: para notificar cuando un jugador se inscribe,
 *    de modo que ms-usuarios actualice el contador de eventos del perfil.
 */
@Service
public class EventoService {

    // Repositorio para la tabla "eventos"
    @Autowired
    private EventoRepository eventoRepository;

    // Repositorio para la tabla "inscripciones"
    @Autowired
    private InscripcionRepository inscripcionRepository;

    // RestTemplate inyectado desde AppConfig para llamar a otros MS
    @Autowired
    private RestTemplate restTemplate;

    // URLs de los MS que consultamos (leidas desde application.properties)
    @Value("${ms.tiendas.url}")
    private String urlMsTiendas;

    @Value("${ms.usuarios.url}")
    private String urlMsUsuarios;

    // =========================================================
    // CREAR EVENTO
    // =========================================================

    /*
     * Crea un nuevo evento organizado por una tienda.
     *
     * Proceso:
     * 1. Consulta ms-tiendas para verificar que la tienda existe y esta ACTIVA.
     * 2. Valida los datos obligatorios del formulario.
     * 3. Guarda el evento en la BD con estado ABIERTO.
     * 4. Notifica a ms-tiendas para que incremente su contador de eventos.
     * 5. Devuelve el evento creado con el nombre de la tienda incluido.
     *
     * @param tiendaId   id de la tienda organizadora (viene de la URL)
     * @param dto        datos del formulario
     * @param authHeader header completo "Bearer eyJ..." para llamar a otros MS
     */
    public EventoRespuestaDto crearEvento(Integer tiendaId, CrearEventoDto dto, String authHeader) {

        // Paso 1: verificar que la tienda existe y esta ACTIVA en ms-tiendas
        Map<String, Object> datosTienda = consultarResumenTienda(tiendaId, authHeader);
        if (datosTienda == null) {
            throw new RuntimeException("No se encontro la tienda con id: " + tiendaId
                    + ". Verifica que ms-tiendas este corriendo.");
        }

        // Solo las tiendas ACTIVAS pueden organizar eventos
        String estadoTienda = (String) datosTienda.get("estado");
        if (!"ACTIVA".equals(estadoTienda)) {
            throw new RuntimeException("La tienda no esta activa. Estado actual: " + estadoTienda
                    + ". Solo las tiendas ACTIVAS pueden crear eventos.");
        }

        // Paso 2: validar los datos obligatorios del formulario
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
        nuevoEvento.setEstado(EstadoEvento.ABIERTO); // siempre empieza en ABIERTO
        nuevoEvento.setFechaInicio(dto.getFechaInicio());
        nuevoEvento.setCuposMaximos(dto.getCuposMaximos());
        // Si no se indica precio, es gratis (0)
        nuevoEvento.setPrecioInscripcion(
                dto.getPrecioInscripcion() != null ? dto.getPrecioInscripcion() : 0);

        Evento eventoGuardado = eventoRepository.save(nuevoEvento);

        // Paso 4: notificar a ms-tiendas para sumar 1 a su contador de eventos
        // Si ms-tiendas no responde, igual seguimos (no es critico)
        notificarNuevoEventoATiendas(tiendaId, authHeader);

        // Paso 5: armar la respuesta con nombre de tienda y cupos disponibles
        String nombreTienda = (String) datosTienda.get("nombre");
        int cuposDisponibles = eventoGuardado.getCuposMaximos(); // recien creado, 0 inscritos
        return construirRespuestaEvento(eventoGuardado, nombreTienda, cuposDisponibles);
    }

    // =========================================================
    // LISTAR EVENTOS ABIERTOS (vista publica)
    // =========================================================

    /*
     * Devuelve todos los eventos con estado ABIERTO.
     * Es la vista que ven los jugadores para buscar torneos.
     *
     * @param authHeader header para consultar nombre de tiendas
     */
    public List<EventoRespuestaDto> listarEventosAbiertos(String authHeader) {

        // Traer solo los eventos ABIERTOS de la BD
        List<Evento> eventosAbiertos = eventoRepository.findByEstado(EstadoEvento.ABIERTO);

        // Convertir cada evento a DTO de respuesta
        List<EventoRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Evento evento : eventosAbiertos) {

            // Consultar el nombre de la tienda para cada evento
            Map<String, Object> datosTienda = consultarResumenTienda(evento.getTiendaId(), authHeader);
            String nombreTienda = datosTienda != null
                    ? (String) datosTienda.get("nombre")
                    : "Tienda desconocida";

            // Calcular cuantos cupos quedan disponibles
            int inscritos = inscripcionRepository.countByEvento(evento);
            int cuposDisponibles = evento.getCuposMaximos() - inscritos;

            listaRespuesta.add(construirRespuestaEvento(evento, nombreTienda, cuposDisponibles));
        }

        return listaRespuesta;
    }

    // =========================================================
    // LISTAR EVENTOS DE UNA TIENDA
    // =========================================================

    /*
     * Devuelve todos los eventos de una tienda especifica.
     * Incluye todos los estados (ABIERTO, CERRADO, FINALIZADO, etc.).
     * Lo usa la tienda para gestionar su historial de eventos.
     *
     * @param tiendaId   id de la tienda
     * @param authHeader header para consultar nombre de la tienda
     */
    public List<EventoRespuestaDto> listarEventosDeTienda(Integer tiendaId, String authHeader) {

        List<Evento> eventos = eventoRepository.findByTiendaId(tiendaId);

        // Consultar el nombre de la tienda una sola vez (es siempre la misma)
        Map<String, Object> datosTienda = consultarResumenTienda(tiendaId, authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

        List<EventoRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Evento evento : eventos) {
            int inscritos = inscripcionRepository.countByEvento(evento);
            int cuposDisponibles = evento.getCuposMaximos() - inscritos;
            listaRespuesta.add(construirRespuestaEvento(evento, nombreTienda, cuposDisponibles));
        }

        return listaRespuesta;
    }

    // =========================================================
    // VER UN EVENTO POR ID
    // =========================================================

    /*
     * Devuelve los datos completos de un evento especifico.
     *
     * @param id         id del evento
     * @param authHeader header para consultar nombre de la tienda
     */
    public EventoRespuestaDto obtenerPorId(Integer id, String authHeader) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));

        Map<String, Object> datosTienda = consultarResumenTienda(evento.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

        int inscritos = inscripcionRepository.countByEvento(evento);
        int cuposDisponibles = evento.getCuposMaximos() - inscritos;

        return construirRespuestaEvento(evento, nombreTienda, cuposDisponibles);
    }

    // =========================================================
    // CAMBIAR ESTADO DE UN EVENTO
    // =========================================================

    /*
     * Cambia el estado de un evento (ABIERTO, CERRADO, EN_CURSO, etc.).
     * Solo la tienda organizadora puede cambiar el estado de sus eventos.
     *
     * @param id          id del evento
     * @param nuevoEstado el nuevo estado a asignar
     * @param usuarioId   id del usuario autenticado (dueno de la tienda, viene del token)
     * @param authHeader  header para consultar la tienda y verificar la propiedad
     */
    public EventoRespuestaDto cambiarEstado(Integer id, EstadoEvento nuevoEstado,
                                             Integer usuarioId, String authHeader) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));

        // El id de la tienda NO es el mismo que el id del usuario dueno,
        // hay que resolver cuales tiendas son del usuario autenticado.
        if (!esTiendaDelUsuario(evento.getTiendaId(), usuarioId, authHeader)) {
            throw new RuntimeException("No tienes permiso para modificar este evento. "
                    + "Solo la tienda organizadora puede cambiarlo.");
        }

        // Cambiar el estado y guardar
        evento.setEstado(nuevoEstado);
        Evento eventoActualizado = eventoRepository.save(evento);

        Map<String, Object> datosTienda = consultarResumenTienda(evento.getTiendaId(), authHeader);
        String nombreTienda = datosTienda != null
                ? (String) datosTienda.get("nombre")
                : "Tienda desconocida";

        int inscritos = inscripcionRepository.countByEvento(eventoActualizado);
        int cuposDisponibles = eventoActualizado.getCuposMaximos() - inscritos;

        return construirRespuestaEvento(eventoActualizado, nombreTienda, cuposDisponibles);
    }

    // =========================================================
    // INSCRIBIRSE A UN EVENTO
    // =========================================================

    /*
     * Inscribe al usuario autenticado en un evento.
     *
     * Proceso:
     * 1. Verifica que el evento existe y esta ABIERTO.
     * 2. Verifica que el usuario no este ya inscrito.
     * 3. Verifica que queden cupos disponibles.
     * 4. Guarda la inscripcion en la BD.
     * 5. Notifica a ms-usuarios para sumar 1 al contador de eventos del perfil.
     *
     * @param eventoId   id del evento
     * @param usuarioId  id del jugador autenticado (viene del token)
     * @param nombre     nombre del jugador (viene del token)
     * @param authHeader header para notificar a ms-usuarios
     */
    public InscripcionRespuestaDto inscribirse(Integer eventoId, Integer usuarioId,
                                                String nombre, String authHeader) {

        // Paso 1: buscar el evento y verificar que este ABIERTO
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventoId));

        if (!EstadoEvento.ABIERTO.equals(evento.getEstado())) {
            throw new RuntimeException("Este evento no acepta inscripciones. "
                    + "Estado actual: " + evento.getEstado());
        }

        // Paso 2: verificar que el usuario no este ya inscrito en este evento
        if (inscripcionRepository.findByEventoAndUsuarioId(evento, usuarioId).isPresent()) {
            throw new RuntimeException("Ya estas inscrito en este evento.");
        }

        // Paso 3: contar los inscritos y verificar que queden cupos
        int totalInscritos = inscripcionRepository.countByEvento(evento);
        if (totalInscritos >= evento.getCuposMaximos()) {
            throw new RuntimeException("Lo sentimos, no quedan cupos disponibles para este evento.");
        }

        // Paso 4: crear y guardar la inscripcion
        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setEvento(evento);
        nuevaInscripcion.setUsuarioId(usuarioId);
        nuevaInscripcion.setFechaInscripcion(LocalDateTime.now());
        nuevaInscripcion.setConfirmado(false);

        Inscripcion guardada = inscripcionRepository.save(nuevaInscripcion);

        // Paso 5: notificar a ms-usuarios para actualizar el contador de eventos del jugador
        // Si ms-usuarios no responde, igual seguimos (no es critico para la inscripcion)
        notificarInscripcionAUsuarios(usuarioId, authHeader);

        // Armar la respuesta con los datos de la inscripcion
        return construirRespuestaInscripcion(guardada, nombre);
    }

    // =========================================================
    // VER MIS INSCRIPCIONES (historial del jugador)
    // =========================================================

    /*
     * Devuelve todos los eventos a los que se inscribio el usuario autenticado.
     *
     * @param usuarioId  id del jugador
     * @param nombre     nombre del jugador (viene del token)
     */
    public List<InscripcionRespuestaDto> misInscripciones(Integer usuarioId, String nombre) {

        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(usuarioId);

        List<InscripcionRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            listaRespuesta.add(construirRespuestaInscripcion(inscripcion, nombre));
        }

        return listaRespuesta;
    }

    // =========================================================
    // VER PARTICIPANTES DE UN EVENTO
    // =========================================================

    /*
     * Devuelve la lista de jugadores inscritos en un evento.
     * Solo la tienda organizadora puede ver la lista completa.
     *
     * @param eventoId   id del evento
     * @param usuarioId  id del usuario autenticado (dueno de la tienda, viene del token)
     * @param authHeader header para consultar la tienda y verificar la propiedad
     */
    public List<InscripcionRespuestaDto> verParticipantes(Integer eventoId, Integer usuarioId, String authHeader) {

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventoId));

        // El id de la tienda NO es el mismo que el id del usuario dueno,
        // hay que resolver cuales tiendas son del usuario autenticado.
        if (!esTiendaDelUsuario(evento.getTiendaId(), usuarioId, authHeader)) {
            throw new RuntimeException("Solo la tienda organizadora puede ver la lista de participantes.");
        }

        List<Inscripcion> inscripciones = inscripcionRepository.findByEvento(evento);

        List<InscripcionRespuestaDto> listaRespuesta = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            // Sin nombre porque no tenemos datos de ms-login aqui
            listaRespuesta.add(construirRespuestaInscripcion(inscripcion, "Jugador #" + inscripcion.getUsuarioId()));
        }

        return listaRespuesta;
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    /*
     * Llama a ms-tiendas para obtener el nombre y estado de una tienda.
     * Devuelve un Map con los datos o null si ms-tiendas no responde.
     *
     * @param tiendaId   id de la tienda a consultar
     * @param authHeader header completo "Bearer eyJ..." para la peticion
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> consultarResumenTienda(Integer tiendaId, String authHeader) {
        try {
            String url = urlMsTiendas + "/api/tiendas/" + tiendaId + "/resumen";

            // Crear el header con el token JWT para que ms-tiendas autorice la peticion
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> peticion = new HttpEntity<>(headers);

            // Hacer la peticion GET y mapear la respuesta JSON a un Map de Java
            ResponseEntity<Map> respuesta = restTemplate.exchange(
                    url, HttpMethod.GET, peticion, Map.class);

            return respuesta.getBody();

        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo consultar ms-tiendas para tienda "
                    + tiendaId + ": " + e.getMessage());
            return null;
        }
    }

    /*
     * Verifica si una tienda (por su id real en ms-tiendas) pertenece
     * al usuario autenticado. El id de la tienda es distinto del id
     * del usuario dueno, por lo que hay que resolverlo consultando
     * ms-tiendas en lugar de comparar los ids directamente.
     *
     * @param tiendaId   id de la tienda organizadora del evento
     * @param usuarioId  id del usuario autenticado (viene del token)
     * @param authHeader header completo "Bearer eyJ..." para la peticion
     */
    @SuppressWarnings("unchecked")
    private boolean esTiendaDelUsuario(Integer tiendaId, Integer usuarioId, String authHeader) {
        try {
            String url = urlMsTiendas + "/api/tiendas/dueno/" + usuarioId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> peticion = new HttpEntity<>(headers);

            ResponseEntity<List> respuesta = restTemplate.exchange(
                    url, HttpMethod.GET, peticion, List.class);

            List<Map<String, Object>> tiendasDelUsuario = respuesta.getBody();
            if (tiendasDelUsuario == null) {
                return false;
            }

            for (Map<String, Object> tienda : tiendasDelUsuario) {
                Object id = tienda.get("id");
                if (id != null && tiendaId.equals(((Number) id).intValue())) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo verificar el dueno de la tienda "
                    + tiendaId + ": " + e.getMessage());
            return false;
        }
    }

    /*
     * Notifica a ms-tiendas que se creo un evento nuevo.
     * ms-tiendas incrementa el contador de eventos en sus metricas.
     *
     * Si ms-tiendas no responde, solo se imprime el error.
     * La creacion del evento sigue adelante igual.
     */
    private void notificarNuevoEventoATiendas(Integer tiendaId, String authHeader) {
        try {
            String url = urlMsTiendas + "/api/tiendas/" + tiendaId + "/sumar-evento";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> peticion = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.PUT, peticion, Void.class);
        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo notificar a ms-tiendas: " + e.getMessage());
        }
    }

    /*
     * Notifica a ms-usuarios que el jugador se inscribio a un evento.
     * ms-usuarios incrementa el contador de eventos en el perfil del jugador.
     *
     * Si ms-usuarios no responde, la inscripcion sigue adelante igual.
     */
    private void notificarInscripcionAUsuarios(Integer usuarioId, String authHeader) {
        try {
            String url = urlMsUsuarios + "/api/perfiles/sumar-evento/" + usuarioId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> peticion = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.PUT, peticion, Void.class);
        } catch (Exception e) {
            System.out.println("[ms-eventos] No se pudo notificar a ms-usuarios: " + e.getMessage());
        }
    }

    // Convierte una entidad Evento al DTO de respuesta
    private EventoRespuestaDto construirRespuestaEvento(Evento evento, String nombreTienda,
                                                          int cuposDisponibles) {
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

    // Convierte una entidad Inscripcion al DTO de respuesta
    private InscripcionRespuestaDto construirRespuestaInscripcion(Inscripcion inscripcion,
                                                                    String nombreUsuario) {
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
