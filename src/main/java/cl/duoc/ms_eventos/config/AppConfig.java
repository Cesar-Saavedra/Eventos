package cl.duoc.ms_eventos.config;

import org.springframework.context.annotation.Configuration;

/*
 * Configuracion general de ms-eventos.
 * El bean RestTemplate fue eliminado: la comunicacion con ms-tiendas y ms-usuarios
 * ahora se hace via Feign (TiendaFeignClient y UsuarioFeignClient),
 * que usan Eureka para resolver los hosts sin URLs hardcodeadas.
 */
@Configuration
public class AppConfig {
    // Vacio: Feign no necesita un bean RestTemplate manual.
}
