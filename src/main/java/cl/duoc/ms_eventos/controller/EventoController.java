package cl.duoc.ms_eventos.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_eventos.dto.CrearEventoDto;
import cl.duoc.ms_eventos.dto.EventoRespuestaDto;
import cl.duoc.ms_eventos.dto.InscripcionRespuestaDto;
import cl.duoc.ms_eventos.model.EstadoEvento;
import cl.duoc.ms_eventos.security.JwtUtil;
import cl.duoc.ms_eventos.service.EventoService;
import jakarta.validation.Valid;

/*
 * Controlador REST de ms-eventos.
 * Puerto: 8087
 * Base URL: http://localhost:8087/api/eventos
 *
 * Arquitectura: Controller -> Service -> Repository (CSR)
 * Este controlador solo valida el token y delega al servicio.
 *
 * TODOS los endpoints requieren:
 *   Header: Authorization: Bearer {token}
 *
 * -------------------------------------------------------
 * ENDPOINTS PARA JUGADORES (cualquier rol):
 * -------------------------------------------------------
 * GET /api/eventos
 *     -> Ver todos los eventos abiertos a inscripciones
 *
 * GET /api/eventos/{id}
 *     -> Ver los datos completos de un evento
 *
 * POST /api/eventos/{id}/inscribirse
 *     -> Inscribirse en un evento
 *
 * GET /api/eventos/mis-inscripciones
 *     -> Ver los eventos en los que estoy inscrito
 *
 * -------------------------------------------------------
 * ENDPOINTS PARA TIENDAS (solo rol TIENDA):
 * -------------------------------------------------------
 * POST /api/eventos/tienda/{tiendaId}
 *     -> Crear un nuevo evento para mi tienda
 *
 * GET /api/eventos/tienda/{tiendaId}
 *     -> Ver todos los eventos de mi tienda
 *
 * PUT /api/eventos/{id}/estado
 *     -> Cambiar el estado de mi evento (CERRADO, EN_CURSO, etc.)
 *
 * GET /api/eventos/{id}/participantes
 *     -> Ver la lista de jugadores inscritos en mi evento
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private JwtUtil jwtUtil;

    // =========================================================
    // GET /api/eventos
    // Ver todos los eventos abiertos (vista publica)
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     *
     * Devuelve todos los eventos con estado ABIERTO.
     * Cualquier usuario autenticado puede verlos.
     *
     * Respuesta 200: lista de EventoRespuestaDto
     *
     * Ejemplo en Postman:
     * GET http://localhost:8087/api/eventos
     */
    @GetMapping
    public ResponseEntity<?> listarEventosAbiertos(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para ver los eventos.");
        }

        try {
            List<EventoRespuestaDto> eventos = eventoService.listarEventosAbiertos(authHeader);
            return ResponseEntity.ok(eventos);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // GET /api/eventos/{id}
    // Ver los datos de un evento especifico
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id del evento
     *
     * Respuesta 200: EventoRespuestaDto con cuposDisponibles calculado
     * Respuesta 404: si el evento no existe
     *
     * Ejemplo en Postman:
     * GET http://localhost:8087/api/eventos/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> verEvento(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        try {
            EventoRespuestaDto evento = eventoService.obtenerPorId(id, authHeader);
            return ResponseEntity.ok(evento);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================
    // POST /api/eventos/tienda/{tiendaId}
    // Crear un nuevo evento para una tienda
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: tiendaId = id de la tienda organizadora
     * Solo usuarios con rol TIENDA pueden crear eventos.
     *
     * Body JSON:
     * {
     *   "nombre": "Torneo Regional Pokemon - Mayo 2026",
     *   "descripcion": "Formato Estandar, cupos limitados.",
     *   "tipoEvento": "TORNEO",
     *   "fechaInicio": "2026-05-30T14:00:00",
     *   "cuposMaximos": 16,
     *   "precioInscripcion": 3000
     * }
     *
     * Respuesta 201: el evento creado con estado ABIERTO
     */
    @PostMapping("/tienda/{tiendaId}")
    public ResponseEntity<?> crearEvento(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer tiendaId,
            @Valid @RequestBody CrearEventoDto dto) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para crear un evento.");
        }

        // Solo el rol TIENDA puede crear eventos
        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden crear eventos.");
        }

        try {
            EventoRespuestaDto evento = eventoService.crearEvento(tiendaId, dto, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(evento);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // GET /api/eventos/tienda/{tiendaId}
    // Ver todos los eventos de una tienda
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: tiendaId = id de la tienda
     *
     * Devuelve todos los eventos de la tienda (todos los estados).
     * Lo usa la tienda para gestionar su historial de eventos.
     *
     * Ejemplo en Postman:
     * GET http://localhost:8087/api/eventos/tienda/3
     */
    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<?> listarEventosDeTienda(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer tiendaId) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        try {
            List<EventoRespuestaDto> eventos = eventoService.listarEventosDeTienda(tiendaId, authHeader);
            return ResponseEntity.ok(eventos);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // PUT /api/eventos/{id}/estado
    // Cambiar el estado de un evento
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id del evento
     * Query param: nuevoEstado = CERRADO | EN_CURSO | FINALIZADO | CANCELADO
     *
     * Solo la tienda organizadora del evento puede cambiarlo.
     * El tiendaId se extrae del token, no del body.
     *
     * Ejemplo en Postman:
     * PUT http://localhost:8087/api/eventos/1/estado?nuevoEstado=CERRADO
     *
     * Respuesta 200: el evento con el nuevo estado
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id,
            @RequestParam EstadoEvento nuevoEstado) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        // Solo las tiendas pueden cambiar el estado de sus eventos
        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden cambiar el estado de un evento.");
        }

        // El id del usuario autenticado (dueno de la tienda) viene del token.
        // No es el mismo id que el de la tienda: el servicio resuelve la
        // tienda real consultando a ms-tiendas antes de verificar el permiso.
        Integer usuarioId = jwtUtil.extraerId(token);

        try {
            EventoRespuestaDto evento = eventoService.cambiarEstado(id, nuevoEstado, usuarioId, authHeader);
            return ResponseEntity.ok(evento);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // POST /api/eventos/{id}/inscribirse
    // Inscribirse en un evento
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id del evento
     *
     * El usuarioId y nombre se extraen del token.
     * El jugador no puede inscribirse dos veces al mismo evento.
     *
     * Respuesta 201: la inscripcion creada
     * Respuesta 400: si ya esta inscrito, no hay cupos o el evento no esta abierto
     *
     * Ejemplo en Postman:
     * POST http://localhost:8087/api/eventos/1/inscribirse
     * (sin body, solo el header Authorization)
     */
    @PostMapping("/{id}/inscribirse")
    public ResponseEntity<?> inscribirse(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido para inscribirse.");
        }

        // Extraer los datos del jugador directamente del token
        Integer usuarioId = jwtUtil.extraerId(token);
        String  nombre    = jwtUtil.extraerNombre(token);

        try {
            InscripcionRespuestaDto inscripcion = eventoService.inscribirse(
                    id, usuarioId, nombre, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(inscripcion);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // GET /api/eventos/mis-inscripciones
    // Ver mis inscripciones (historial del jugador)
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     *
     * Devuelve todos los eventos a los que se inscribio el usuario autenticado.
     * Cualquier rol puede usarlo (JUGADOR, TIENDA, ORGANIZADOR).
     *
     * Respuesta 200: lista de InscripcionRespuestaDto
     *
     * Ejemplo en Postman:
     * GET http://localhost:8087/api/eventos/mis-inscripciones
     */
    @GetMapping("/mis-inscripciones")
    public ResponseEntity<?> misInscripciones(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        Integer usuarioId = jwtUtil.extraerId(token);
        String  nombre    = jwtUtil.extraerNombre(token);

        try {
            List<InscripcionRespuestaDto> inscripciones = eventoService.misInscripciones(usuarioId, nombre);
            return ResponseEntity.ok(inscripciones);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // GET /api/eventos/{id}/participantes
    // Ver lista de jugadores inscritos en un evento
    // =========================================================
    /*
     * Header: Authorization: Bearer {token}
     * Path param: id = id del evento
     *
     * Solo la tienda organizadora puede ver la lista de participantes.
     * El tiendaId se extrae del token del usuario autenticado.
     *
     * Respuesta 200: lista de InscripcionRespuestaDto
     *
     * Ejemplo en Postman:
     * GET http://localhost:8087/api/eventos/1/participantes
     */
    @GetMapping("/{id}/participantes")
    public ResponseEntity<?> verParticipantes(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id) {

        String token = validarHeader(authHeader);
        if (token == null) {
            return respuestaNoAutorizado("Token requerido.");
        }

        String rol = jwtUtil.extraerRol(token);
        if (!"TIENDA".equals(rol)) {
            return respuestaNoAutorizado("Solo los usuarios con rol TIENDA pueden ver la lista de participantes.");
        }

        Integer usuarioId = jwtUtil.extraerId(token);

        try {
            List<InscripcionRespuestaDto> participantes =
                    eventoService.verParticipantes(id, usuarioId, authHeader);
            return ResponseEntity.ok(participantes);
        } catch (RuntimeException e) {
            return respuestaError(e.getMessage());
        }
    }

    // =========================================================
    // METODOS PRIVADOS DE AYUDA
    // =========================================================

    // Valida el header y devuelve el token limpio, o null si es invalido
    private String validarHeader(String authHeader) {
        String token = jwtUtil.obtenerTokenDelHeader(authHeader);
        if (token == null || !jwtUtil.esTokenValido(token)) {
            return null;
        }
        return token;
    }

    // Respuesta estandar 401 Unauthorized
    private ResponseEntity<?> respuestaNoAutorizado(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Respuesta estandar 400 Bad Request para errores de negocio
    private ResponseEntity<?> respuestaError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
