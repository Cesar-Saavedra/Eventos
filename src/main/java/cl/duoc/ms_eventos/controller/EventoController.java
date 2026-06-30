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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 */
@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Gestión de eventos y torneos organizados por tiendas")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "Listar eventos abiertos", description = "Devuelve todos los eventos con estado ABIERTO disponibles para inscripción.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos abiertos obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> listarEventosAbiertos(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
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

    @GetMapping("/{id}")
    @Operation(summary = "Ver evento por ID", description = "Devuelve los datos completos de un evento, incluyendo cupos disponibles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    public ResponseEntity<?> verEvento(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del evento a consultar", required = true, example = "1")
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

    @PostMapping("/tienda/{tiendaId}")
    @Operation(summary = "Crear evento", description = "Crea un nuevo torneo o evento para la tienda indicada. Solo rol TIENDA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado con estado ABIERTO", content = @Content(
                    examples = @ExampleObject(name = "EventoCreado", value = """
                            {
                              "id": 1,
                              "nombre": "Torneo Regional Pokemon - Mayo 2026",
                              "descripcion": "Formato Estandar, cupos limitados.",
                              "tipoEvento": "TORNEO",
                              "fechaInicio": "2026-05-30T14:00:00",
                              "cuposMaximos": 16,
                              "cuposDisponibles": 16,
                              "precioInscripcion": 3000,
                              "estado": "ABIERTO"
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o rol distinto de TIENDA")
    })
    public ResponseEntity<?> crearEvento(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a un usuario con rol TIENDA", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la tienda organizadora", required = true, example = "3")
            @PathVariable Integer tiendaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del evento a crear", required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "nombre": "Torneo Regional Pokemon - Mayo 2026",
                              "descripcion": "Formato Estandar, cupos limitados.",
                              "tipoEvento": "TORNEO",
                              "fechaInicio": "2026-05-30T14:00:00",
                              "cuposMaximos": 16,
                              "precioInscripcion": 3000
                            }
                            """)))
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

    @GetMapping("/tienda/{tiendaId}")
    @Operation(summary = "Eventos de una tienda", description = "Devuelve todos los eventos de la tienda (todos los estados). Útil para gestión del historial.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos de la tienda"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> listarEventosDeTienda(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la tienda", required = true, example = "3")
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

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del evento", description = "Cambia el estado del evento (CERRADO, EN_CURSO, FINALIZADO, CANCELADO). Solo la tienda organizadora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento con el nuevo estado"),
            @ApiResponse(responseCode = "400", description = "Transición de estado inválida"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o el usuario no es dueño del evento")
    })
    public ResponseEntity<?> cambiarEstado(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a la tienda organizadora", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del evento", required = true, example = "1")
            @PathVariable Integer id,
            @Parameter(description = "Nuevo estado del evento", required = true, example = "CERRADO")
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

    @PostMapping("/{id}/inscribirse")
    @Operation(summary = "Inscribirse en evento", description = "El usuario autenticado se inscribe en el evento. No se puede inscribir dos veces.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscripción creada"),
            @ApiResponse(responseCode = "400", description = "Ya está inscrito, no hay cupos o el evento no está abierto"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> inscribirse(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del evento al que se desea inscribir", required = true, example = "1")
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

    @GetMapping("/mis-inscripciones")
    @Operation(summary = "Mis inscripciones", description = "Historial de todos los eventos en los que se inscribió el usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de inscripciones del usuario"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    public ResponseEntity<?> misInscripciones(
            @Parameter(description = "Token JWT con formato 'Bearer {token}'", required = true)
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

    @GetMapping("/{id}/participantes")
    @Operation(summary = "Ver participantes", description = "Lista de jugadores inscritos en el evento. Solo la tienda organizadora puede consultarlo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de participantes del evento"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, o el usuario no es la tienda organizadora")
    })
    public ResponseEntity<?> verParticipantes(
            @Parameter(description = "Token JWT con formato 'Bearer {token}', debe pertenecer a la tienda organizadora", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID del evento", required = true, example = "1")
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
