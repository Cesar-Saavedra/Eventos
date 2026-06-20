package cl.duoc.ms_eventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// @EnableFeignClients activa el escaneo de interfaces @FeignClient en este paquete
@SpringBootApplication
@EnableFeignClients
public class MsEventosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsEventosApplication.class, args);
    }
}
