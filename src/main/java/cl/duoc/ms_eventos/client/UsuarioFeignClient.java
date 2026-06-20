package cl.duoc.ms_eventos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/*
 * Cliente Feign para comunicarse con ms-usuarios.
 *
 * name = "ms-usuarios" → Feign resuelve el host via Eureka (lb://ms-usuarios),
 * sin URLs hardcodeadas en el yaml.
 *
 * ms-eventos llama a ms-usuarios para notificar que un jugador
 * se inscribio en un evento (ms-usuarios actualiza el contador del perfil).
 */
@FeignClient(name = "ms-usuarios")
public interface UsuarioFeignClient {

    /*
     * PUT /api/perfil/sumar-evento/{usuarioId}
     * Incrementa el contador de eventos del perfil del jugador.
     * Se llama cuando un jugador se inscribe exitosamente a un evento.
     *
     * Nota: este endpoint debe existir en ms-usuarios (PerfilController).
     */
    @PutMapping("/api/perfil/sumar-evento/{usuarioId}")
    void sumarEvento(
            @PathVariable("usuarioId") Integer usuarioId,
            @RequestHeader("Authorization") String authHeader
    );
}
