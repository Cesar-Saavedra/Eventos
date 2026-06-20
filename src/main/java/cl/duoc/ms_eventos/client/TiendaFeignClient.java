package cl.duoc.ms_eventos.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import cl.duoc.ms_eventos.dto.TiendaResumenDTO;

/*
 * Cliente Feign para comunicarse con ms-tiendas.
 *
 * name = "ms-tiendas" → Feign resuelve el host via Eureka (lb://ms-tiendas),
 * sin URLs hardcodeadas en el yaml.
 *
 * ms-eventos llama a ms-tiendas para:
 * 1. Verificar que la tienda existe y esta ACTIVA antes de crear un evento.
 * 2. Obtener las tiendas de un usuario para verificar propiedad.
 * 3. Notificar que se creo un evento (sumar 1 al contador de eventos).
 */
@FeignClient(name = "ms-tiendas")
public interface TiendaFeignClient {

    /*
     * GET /api/tiendas/{id}/resumen
     * Devuelve nombre y estado de una tienda.
     */
    @GetMapping("/api/tiendas/{id}/resumen")
    TiendaResumenDTO obtenerResumenTienda(
            @PathVariable("id") Integer idTienda,
            @RequestHeader("Authorization") String authHeader
    );

    /*
     * GET /api/tiendas/dueno/{usuarioId}
     * Devuelve todas las tiendas que pertenecen a un usuario.
     * Se usa para verificar si el usuario autenticado es dueno de la tienda
     * organizadora de un evento (el id de tienda != id de usuario).
     */
    @GetMapping("/api/tiendas/dueno/{usuarioId}")
    List<TiendaResumenDTO> obtenerTiendasPorDueno(
            @PathVariable("usuarioId") Integer usuarioId,
            @RequestHeader("Authorization") String authHeader
    );

    /*
     * PUT /api/tiendas/{id}/sumar-evento
     * Incrementa el contador de eventos de la tienda en sus metricas.
     * Se llama cuando una tienda crea un nuevo evento/torneo.
     */
    @PutMapping("/api/tiendas/{id}/sumar-evento")
    void sumarEvento(
            @PathVariable("id") Integer idTienda,
            @RequestHeader("Authorization") String authHeader
    );
}
