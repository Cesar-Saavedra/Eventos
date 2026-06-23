package cl.duoc.ms_eventos.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cl.duoc.ms_eventos.client.TiendaFeignClient;
import cl.duoc.ms_eventos.client.UsuarioFeignClient;
import cl.duoc.ms_eventos.dto.InscripcionRespuestaDto;
import cl.duoc.ms_eventos.model.Evento;
import cl.duoc.ms_eventos.model.Inscripcion;
import cl.duoc.ms_eventos.model.TipoEvento;
import cl.duoc.ms_eventos.repository.EventoRepository;
import cl.duoc.ms_eventos.repository.InscripcionRepository;

// NOTA: salvo misInscripciones, todos los demas metodos de EventoService
// dependen internamente de TiendaFeignClient y/o UsuarioFeignClient para
// verificar/notificar a otros microservicios, por lo que quedan fuera del
// alcance de estas pruebas unitarias, por decision del equipo.
@ExtendWith(MockitoExtension.class)
public class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private TiendaFeignClient tiendaFeignClient;

    @Mock
    private UsuarioFeignClient usuarioFeignClient;

    @InjectMocks
    private EventoService eventoService;

    private Evento eventoEjemplo;

    @BeforeEach
    void setUp(){
        eventoEjemplo = new Evento();
        eventoEjemplo.setId(1);
        eventoEjemplo.setNombre("Torneo Regional Pokemon");
        eventoEjemplo.setTiendaId(3);
        eventoEjemplo.setTipoEvento(TipoEvento.TORNEO);
        eventoEjemplo.setFechaInicio(LocalDateTime.now().plusDays(5));
        eventoEjemplo.setCuposMaximos(16);
        eventoEjemplo.setPrecioInscripcion(0);
    }

    // =====================================================================
    // misInscripciones
    // =====================================================================

    @Test
    void misInscripciones_retornaLista(){
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(1);
        inscripcion.setEvento(eventoEjemplo);
        inscripcion.setUsuarioId(7);
        inscripcion.setFechaInscripcion(LocalDateTime.now());
        inscripcion.setConfirmado(false);

        when(inscripcionRepository.findByUsuarioId(7)).thenReturn(Arrays.asList(inscripcion));

        List<InscripcionRespuestaDto> resultado = eventoService.misInscripciones(7, "DarkMage99");

        assertEquals(1, resultado.size());
        assertEquals("Torneo Regional Pokemon", resultado.get(0).getNombreEvento());
        assertEquals("DarkMage99", resultado.get(0).getNombreUsuario());
    }

    @Test
    void misInscripciones_sinInscripciones_retornaListaVacia(){
        when(inscripcionRepository.findByUsuarioId(99)).thenReturn(Arrays.asList());

        List<InscripcionRespuestaDto> resultado = eventoService.misInscripciones(99, "Nadie");

        assertEquals(0, resultado.size());
    }
}
