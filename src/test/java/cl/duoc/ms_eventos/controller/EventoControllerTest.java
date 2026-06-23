package cl.duoc.ms_eventos.controller;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.duoc.ms_eventos.dto.CrearEventoDto;
import cl.duoc.ms_eventos.dto.EventoRespuestaDto;
import cl.duoc.ms_eventos.dto.InscripcionRespuestaDto;
import cl.duoc.ms_eventos.model.EstadoEvento;
import cl.duoc.ms_eventos.model.TipoEvento;
import cl.duoc.ms_eventos.security.JwtUtil;
import cl.duoc.ms_eventos.service.EventoService;

@WebMvcTest(EventoController.class)
public class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoService eventoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private EventoRespuestaDto eventoEjemplo;

    @BeforeEach
    void setUp(){
        eventoEjemplo = new EventoRespuestaDto(
                1, "Torneo Regional Pokemon", "Descripcion", 3, "Carta Magica TCG",
                TipoEvento.TORNEO, EstadoEvento.ABIERTO, LocalDateTime.now().plusDays(5),
                16, 10, 0
        );
    }

    // =====================================================================
    // GET /api/eventos
    // =====================================================================

    @Test
    void listarEventosAbiertos_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/eventos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarEventosAbiertos_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(eventoService.listarEventosAbiertos(anyString())).thenReturn(Arrays.asList(eventoEjemplo));

        mockMvc.perform(get("/api/eventos").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Torneo Regional Pokemon"));
    }

    // =====================================================================
    // GET /api/eventos/{id}
    // =====================================================================

    @Test
    void verEvento_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/eventos/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verEvento_encontrado_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(eventoService.obtenerPorId(1, "Bearer token-bueno")).thenReturn(eventoEjemplo);

        mockMvc.perform(get("/api/eventos/1").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void verEvento_noEncontrado_retorna404() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(eventoService.obtenerPorId(99, "Bearer token-bueno"))
                .thenThrow(new RuntimeException("Evento no encontrado con id: 99"));

        mockMvc.perform(get("/api/eventos/99").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Evento no encontrado con id: 99"));
    }

    // =====================================================================
    // POST /api/eventos/tienda/{tiendaId}
    // =====================================================================

    @Test
    void crearEvento_sinToken_retorna401() throws Exception {
        CrearEventoDto dto = new CrearEventoDto();
        dto.setNombre("Torneo");
        dto.setTipoEvento(TipoEvento.TORNEO);
        dto.setFechaInicio(LocalDateTime.now().plusDays(5));
        dto.setCuposMaximos(16);

        mockMvc.perform(post("/api/eventos/tienda/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearEvento_rolNoEsTienda_retorna401() throws Exception {
        CrearEventoDto dto = new CrearEventoDto();
        dto.setNombre("Torneo");
        dto.setTipoEvento(TipoEvento.TORNEO);
        dto.setFechaInicio(LocalDateTime.now().plusDays(5));
        dto.setCuposMaximos(16);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-jugador")).thenReturn("token-jugador");
        when(jwtUtil.esTokenValido("token-jugador")).thenReturn(true);
        when(jwtUtil.extraerRol("token-jugador")).thenReturn("JUGADOR");

        mockMvc.perform(post("/api/eventos/tienda/3")
                        .header("Authorization", "Bearer token-jugador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearEvento_exitoso_retorna201() throws Exception {
        CrearEventoDto dto = new CrearEventoDto();
        dto.setNombre("Torneo Regional Pokemon");
        dto.setTipoEvento(TipoEvento.TORNEO);
        dto.setFechaInicio(LocalDateTime.now().plusDays(5));
        dto.setCuposMaximos(16);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(eventoService.crearEvento(eq(3), any(CrearEventoDto.class), anyString())).thenReturn(eventoEjemplo);

        mockMvc.perform(post("/api/eventos/tienda/3")
                        .header("Authorization", "Bearer token-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crearEvento_errorDeNegocio_retorna400() throws Exception {
        CrearEventoDto dto = new CrearEventoDto();
        dto.setNombre("Torneo Regional Pokemon");
        dto.setTipoEvento(TipoEvento.TORNEO);
        dto.setFechaInicio(LocalDateTime.now().plusDays(5));
        dto.setCuposMaximos(16);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(eventoService.crearEvento(eq(3), any(CrearEventoDto.class), anyString()))
                .thenThrow(new RuntimeException("La tienda no esta activa. Estado actual: PENDIENTE."));

        mockMvc.perform(post("/api/eventos/tienda/3")
                        .header("Authorization", "Bearer token-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // GET /api/eventos/tienda/{tiendaId}
    // =====================================================================

    @Test
    void listarEventosDeTienda_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/eventos/tienda/3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarEventosDeTienda_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(eventoService.listarEventosDeTienda(eq(3), anyString())).thenReturn(Arrays.asList(eventoEjemplo));

        mockMvc.perform(get("/api/eventos/tienda/3").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tiendaId").value(3));
    }

    // =====================================================================
    // PUT /api/eventos/{id}/estado
    // =====================================================================

    @Test
    void cambiarEstado_sinToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/eventos/1/estado").param("nuevoEstado", "CERRADO"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarEstado_rolNoEsTienda_retorna401() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-jugador")).thenReturn("token-jugador");
        when(jwtUtil.esTokenValido("token-jugador")).thenReturn(true);
        when(jwtUtil.extraerRol("token-jugador")).thenReturn("JUGADOR");

        mockMvc.perform(put("/api/eventos/1/estado")
                        .param("nuevoEstado", "CERRADO")
                        .header("Authorization", "Bearer token-jugador"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarEstado_exitoso_retorna200() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(jwtUtil.extraerId("token-tienda")).thenReturn(5);
        when(eventoService.cambiarEstado(eq(1), eq(EstadoEvento.CERRADO), eq(5), anyString()))
                .thenReturn(eventoEjemplo);

        mockMvc.perform(put("/api/eventos/1/estado")
                        .param("nuevoEstado", "CERRADO")
                        .header("Authorization", "Bearer token-tienda"))
                .andExpect(status().isOk());
    }

    // =====================================================================
    // POST /api/eventos/{id}/inscribirse
    // =====================================================================

    @Test
    void inscribirse_sinToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/eventos/1/inscribirse"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inscribirse_exitoso_retorna201() throws Exception {
        InscripcionRespuestaDto inscripcion = new InscripcionRespuestaDto(
                1, 1, "Torneo Regional Pokemon", 7, "DarkMage99", LocalDateTime.now(), false);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(7);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("DarkMage99");
        when(eventoService.inscribirse(eq(1), eq(7), eq("DarkMage99"), anyString())).thenReturn(inscripcion);

        mockMvc.perform(post("/api/eventos/1/inscribirse").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(7));
    }

    @Test
    void inscribirse_sinCupos_retorna400() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(7);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("DarkMage99");
        when(eventoService.inscribirse(eq(1), eq(7), eq("DarkMage99"), anyString()))
                .thenThrow(new RuntimeException("Lo sentimos, no quedan cupos disponibles."));

        mockMvc.perform(post("/api/eventos/1/inscribirse").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // GET /api/eventos/mis-inscripciones
    // =====================================================================

    @Test
    void misInscripciones_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/eventos/mis-inscripciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void misInscripciones_retorna200() throws Exception {
        InscripcionRespuestaDto inscripcion = new InscripcionRespuestaDto(
                1, 1, "Torneo Regional Pokemon", 7, "DarkMage99", LocalDateTime.now(), false);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-bueno")).thenReturn("token-bueno");
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(7);
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("DarkMage99");
        when(eventoService.misInscripciones(7, "DarkMage99")).thenReturn(Arrays.asList(inscripcion));

        mockMvc.perform(get("/api/eventos/mis-inscripciones").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(7));
    }

    // =====================================================================
    // GET /api/eventos/{id}/participantes
    // =====================================================================

    @Test
    void verParticipantes_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/eventos/1/participantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verParticipantes_rolNoEsTienda_retorna401() throws Exception {
        when(jwtUtil.obtenerTokenDelHeader("Bearer token-jugador")).thenReturn("token-jugador");
        when(jwtUtil.esTokenValido("token-jugador")).thenReturn(true);
        when(jwtUtil.extraerRol("token-jugador")).thenReturn("JUGADOR");

        mockMvc.perform(get("/api/eventos/1/participantes").header("Authorization", "Bearer token-jugador"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verParticipantes_exitoso_retorna200() throws Exception {
        InscripcionRespuestaDto inscripcion = new InscripcionRespuestaDto(
                1, 1, "Torneo Regional Pokemon", 7, "Jugador #7", LocalDateTime.now(), false);

        when(jwtUtil.obtenerTokenDelHeader("Bearer token-tienda")).thenReturn("token-tienda");
        when(jwtUtil.esTokenValido("token-tienda")).thenReturn(true);
        when(jwtUtil.extraerRol("token-tienda")).thenReturn("TIENDA");
        when(jwtUtil.extraerId("token-tienda")).thenReturn(5);
        when(eventoService.verParticipantes(eq(1), eq(5), anyString())).thenReturn(Arrays.asList(inscripcion));

        mockMvc.perform(get("/api/eventos/1/participantes").header("Authorization", "Bearer token-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(7));
    }
}
