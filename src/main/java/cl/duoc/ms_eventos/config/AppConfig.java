package cl.duoc.ms_eventos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/*
 * Clase de configuracion general del microservicio.
 *
 * Declaramos RestTemplate como @Bean para que Spring lo administre.
 * Esto evita crear un objeto nuevo en cada clase que lo necesite.
 * En su lugar, Spring crea uno solo y lo comparte (inyecta) donde sea necesario.
 *
 * RestTemplate se usa para llamar a ms-tiendas y ms-usuarios via HTTP.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
